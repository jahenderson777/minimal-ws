(ns client.core
  (:require [client.ui :as ui]
            [cognitect.transit :as transit]
            [replicant.dom :as r]
            [nexus.registry :as nxr]))

(defonce ws (atom nil))
(defonce store (atom nil))
(nxr/register-system->state! deref)

(defn encode [data]
  (let [w (transit/writer :json)]
    (transit/write w data)))

(defn decode [s]
  (let [r (transit/reader :json)]
    (transit/read r s)))

(defn connect! []
  (let [socket (js/WebSocket. "/ws")]
    (set! (.-onmessage socket)
          (fn [e] (swap! store merge (decode (.-data e)))))
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

(defn reload-css! []
  (println "reload css")
   (doseq [link (array-seq (.getElementsByTagName js/document "link"))]
     (when (.includes (.-href link) ".css")
       (set! (.-href link) (str (.-href link) "?v=" (js/Date.now))))))

(defn init [] 
  (connect!)
  (js/setTimeout #(send-msg [:ping {:msg "Hello from client"
                                    :at (.toISOString (js/Date.))}])
                 1000)

  (let [el (js/document.getElementById "app")]
    (add-watch store ::render
               (fn [_ _ _ state]
                 (->> (ui/ui state)
                      (r/render el)))))
  
  (r/set-dispatch!
   (fn [dispatch-data actions]
     (nxr/dispatch store dispatch-data actions)))
  
  (reset! store {:foo "bar"}))


(defn ^:dev/after-load re-render []
  (->> (ui/ui @store)
       (r/render (js/document.getElementById "app")))
  (reload-css!))