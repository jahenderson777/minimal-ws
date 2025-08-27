(ns server.core
  (:require [aleph.http :as http]
            [cognitect.transit :as transit]
            [manifold.deferred :as d]
            [manifold.stream :as s]
            [ring.middleware.content-type :refer [wrap-content-type]]
            [ring.middleware.resource :refer [wrap-resource]]
            [server.events :refer [handle-event]])
  (:import [java.io ByteArrayInputStream ByteArrayOutputStream]))

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
  (d/let-flow [conn (d/catch (http/websocket-connection req) (fn [_] nil))]
              (when conn (s/consume #(->> % decode-transit
                                          handle-event
                                          encode-transit
                                          (s/put! conn))
                                    conn)
                    nil)))

(def app (-> #(case (:uri %)
                "/ws" (ws-handler %)
                "/" {:status 200 :headers {"Content-Type" "text/html"}
                     :body (slurp "resources/public/index.html")}
                {:status 404})
             (wrap-resource "public")
             (wrap-content-type)))

(defonce server (atom nil))

(defn -main [& _]
  (reset! server (http/start-server #'app {:port 8087}))
  (println "Server running"))

(comment
  (.close @server)
  (-main)
  )
