(ns client.core
  (:require
   [client.ui :as ui]
   [clojure.string :as str]
   [cognitect.transit :as transit]
   [nexus.registry :as nxr]
   [replicant.dom :as r]))

(defonce ws (atom nil))
(defonce store (atom nil))
(defonce root-el (js/document.getElementById "app"))
(nxr/register-system->state! deref)

(defn encode [data]
  (let [w (transit/writer :json)]
    (transit/write w data)))

(defn decode [s]
  (let [r (transit/reader :json)]
    (transit/read r s)))

(defn connect-ws! []
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
  (doseq [link (array-seq (.getElementsByTagName js/document "link"))]
    (when (str/includes? (.-href link) ".css")
      (let [clean-href (str/replace (.-href link) #"\?v=\d+" "")]
        (set! (.-href link)
              (str clean-href "?v=" (js/Date.now)))))))

(defn init [] 
  (connect-ws!)
  (js/setTimeout #(send-msg [:ping {:msg "Hello from client"
                                    :at (.toISOString (js/Date.))}])
                 1000)
  
  (add-watch store ::render
             (fn [_ _ _ state] 
               (r/render root-el (ui/ui state))))
  
  (r/set-dispatch!
   (fn [dispatch-data actions]
     (nxr/dispatch store dispatch-data actions)))
  
  (reset! store {:foo "bar"}))


(defn ^:dev/after-load re-render [] 
  (r/render root-el (ui/ui @store))
  (reload-css!))