(ns cheffy.server
  (:require [com.stuartsierra.component :as component]
            [clojure.edn :as edn]))

(defn create-system
  [config]
  (component/system-map
    :config config))

(defn -main
  [config-file]
  (let [config (-> config-file (slurp) (edn/read-string))]
    (component/start (create-system config))))
