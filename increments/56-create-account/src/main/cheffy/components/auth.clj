(ns cheffy.components.auth
  (:require [com.stuartsierra.component :as component]
            [cognitect.aws.client.api :as aws])
  (:import (javax.crypto.spec SecretKeySpec)
           (javax.crypto Mac)
           (java.util Base64)))

(defn calculate-secret-hash
  [{:keys [client-id client-secret username]}]
  (let [hmac-sha256-algorithm "HmacSHA256"
        signing-key (SecretKeySpec. (.getBytes client-secret) hmac-sha256-algorithm)
        mac (doto (Mac/getInstance hmac-sha256-algorithm)
              (.init signing-key)
              (.update (.getBytes username)))
        raw-hmac (.doFinal mac (.getBytes client-id))]
    (.encodeToString (Base64/getEncoder) raw-hmac)))

(defrecord Auth [config cognito-idp]

  component/Lifecycle

  (start [component]
    (println ";; Starting Auth")
    (assoc component
      :cognito-idp (aws/client {:api :cognito-idp})))

  (stop [component]
    (println ";; Stopping Auth")
    (assoc component
      :cognito-idp nil)))

(defn service
  [config]
  (map->Auth {:config config}))