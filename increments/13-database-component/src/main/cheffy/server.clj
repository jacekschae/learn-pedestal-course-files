(ns cheffy.server
  (:require [com.stuartsierra.component :as component]
            [clojure.edn :as edn]
            [cheffy.components.api-server :as api-server]))

(defn create-system
  [config]
  (component/system-map
    :config config
    :api-server (api-server/service (:service-map config))))

(defn -main
  [config-file]
  (let [config (-> config-file (slurp) (edn/read-string))]
    (component/start (create-system config))))
