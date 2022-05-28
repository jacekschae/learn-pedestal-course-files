(ns dev
  (:require [clojure.edn :as edn]
            [cheffy.server :as server]
            [com.stuartsierra.component.repl :as cr]))

(defn system [_]
  (-> (-> "src/config/cheffy/development.edn" (slurp) (edn/read-string))
      (server/create-system)))

(cr/set-init system)

(defn start-dev []
  (cr/start))

(defn stop-dev []
  (cr/stop))

(defn restart-dev []
  (cr/reset))

(comment

  (:api-server cr/system)

  (start-dev)

  (restart-dev)

  (stop-dev)

  )