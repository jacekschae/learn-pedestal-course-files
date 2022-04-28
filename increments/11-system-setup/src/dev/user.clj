(ns user
  (:require [io.pedestal.http :as http]
            [clojure.edn :as edn]
            [cheffy.routes :as routes]))

(defonce system-ref (atom nil))

(defn start-dev []
  (let [config (-> "src/config/cheffy/development.edn" slurp edn/read-string)]
    (reset! system-ref
      (-> config
          (assoc ::http/routes routes/routes)
          (http/create-server)
          (http/start))))
  :started)

(defn stop-dev []
  (http/stop @system-ref)
  :stopped)

(defn restart-dev []
  (stop-dev)
  (start-dev)
  :restarted)

(comment

  (start-dev)

  (restart-dev)

  (stop-dev)

  )