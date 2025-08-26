(ns server.core
  (:require
   [aleph.http :as http]
   [cognitect.transit :as transit]
   [manifold.deferred :as d]
   [manifold.stream :as s]
   [ring.middleware.content-type :refer [wrap-content-type]]
   [ring.middleware.not-modified :refer [wrap-not-modified]]
   [ring.middleware.resource :refer [wrap-resource]]
   [server.events :refer [handle-event]])
  (:import
   [java.io ByteArrayInputStream ByteArrayOutputStream]))

(defn encode-transit [data]
  (let [out (ByteArrayOutputStream.)
        writer (transit/writer out :json)]
    (transit/write writer data)
    (.toString out "UTF-8")))

(defn decode-transit [s]
  (let [in (ByteArrayInputStream. (.getBytes s "UTF-8"))
        reader (transit/reader in :json)]
    (transit/read reader)))

(defn ws-handler [req]
  (d/let-flow [conn (d/catch (http/websocket-connection req)
                             (fn [_] nil))]
              (if-not conn
                {:status 400 :body "Expected WebSocket"}
                (do (s/consume
                     (fn [msg]
                       (let [event (decode-transit msg)
                             response (handle-event event)]
                         (s/put! conn (encode-transit response))))
                     conn)
                    ;; return nil because Ring expects a response
                    nil))))

(def app
  (-> (fn [req]
        (case (:uri req)
          "/ws" (ws-handler req)
          ;; fallback: serve index.html
          {:status 200
           :headers {"Content-Type" "text/html"}
           :body (slurp "resources/public/index.html")}))
      (wrap-resource "public")
      (wrap-content-type)
      (wrap-not-modified)))

(defonce server (atom nil))

(defn start! []
  (reset! server (http/start-server app {:port 8087}))
  (println "Server running at http://localhost:8087"))

(defn stop! []
  (when @server
    (@server :timeout 100)
    (reset! server nil)
    (println "Server stopped")))

(defn -main [& _]
  (start!))