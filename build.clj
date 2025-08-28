(ns build
  (:require [clojure.tools.build.api :as b]))

(def lib 'minimal-ws)
(def version (format "1.2.%s" (b/git-count-revs nil)))
(def class-dir "target/classes")
(def uber-file (format "target/%s-%s-standalone.jar" (name lib) version))

;; delay to defer creating the basis
(def basis (delay (b/create-basis {:project "deps.edn"})))

(defn clean [_]
  (b/delete {:path "target"}))

(defn uber [_]
  (clean nil)
  ;; copy source and resources
  (b/copy-dir {:src-dirs ["src" "resources"]
               :target-dir class-dir})
  ;; compile your main namespace
  (b/compile-clj {:basis @basis
                  :ns-compile '[server.core]
                  :class-dir class-dir})
  ;; build the uberjar
  (b/uber {:class-dir class-dir
           :uber-file uber-file
           :basis @basis
           :main 'server.core}))
