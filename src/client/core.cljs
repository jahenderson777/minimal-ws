(ns client.core
  (:require [client.ui :as ui]
            [clojure.string :as str]
            [cognitect.transit :as transit]
            [nexus.registry :as nxr]
            [replicant.dom :as r]))

(defonce ws (atom nil))
(defonce store (atom nil))

(defn encode [data]
  (let [w (transit/writer :json)]
    (transit/write w data)))

(defn decode [s]
  (let [r (transit/reader :json)]
    (transit/read r s)))

(defn connect-ws! []
  (let [socket (js/WebSocket. "/ws")]
    (set! (.-onmessage socket) 
          #(nxr/dispatch store {} (decode (.-data %))))
    (reset! ws socket)
    (js/console.log "WebSocket connected")))

(defn send-msg [msg]
  (when-let [s @ws]
    (.send s (encode msg))))

(nxr/register-effect! :assoc-in
                      ^:nexus/batch
                      (fn [_ store path-values]
                        (swap! store
                               (fn [state]
                                 (reduce (fn [s [path value]]
                                           (assoc-in s path value)) state path-values)))))

(nxr/register-effect! :ws
                      (fn [_ _store server-evt]
                        (send-msg server-evt)))

(nxr/register-placeholder! :fmt/number
                           (fn [_ value]
                             (or (some-> value parse-long) 0)))

(nxr/register-placeholder! :event.target/value
                           (fn [{:replicant/keys [dom-event]}]
                             (some-> dom-event .-target .-value)))

(defn reload-css! []
  (doseq [link (array-seq (.getElementsByTagName js/document "link"))]
    (when (str/includes? (.-href link) "atomic-dynamic.css")
      (set! (.-href link)
            (str/replace (.-href link) #"\?v=\d+" (str "?v=" (js/Date.now)))))))

(defn ^:dev/after-load re-render []
  (r/render js/document.body (ui/ui @store))
  (reload-css!))

(defn init [] 
  (connect-ws!) 
  (nxr/register-system->state! deref)
  (r/set-dispatch! #(nxr/dispatch store %1 %2))
  (add-watch store ::render #(r/render js/document.body (ui/ui %4))) 
  (reset! store {:foo "bar"}))