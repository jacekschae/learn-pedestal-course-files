(ns dev
  (:require [clojure.edn :as edn]
            [cheffy.server :as server]
            [com.stuartsierra.component.repl :as cr]
            [io.pedestal.http :as http]
            [io.pedestal.test :as pt]
            [datomic.client.api :as d]
            [cognitect.transit :as transit]
            [cognitect.aws.client.api :as aws])
  (:import (java.util Date Base64)
           (javax.crypto Mac)
           (javax.crypto.spec SecretKeySpec)))

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

 (:auth cr/system)

(defn calculate-secret-hash
  [{:keys [client-id client-secret username]}]
  (let [hmac-sha256-algorithm "HmacSHA256"
        signing-key (SecretKeySpec. (.getBytes client-secret) hmac-sha256-algorithm)
        mac (doto (Mac/getInstance hmac-sha256-algorithm)
              (.init signing-key)
              (.update (.getBytes username)))
        raw-hmac (.doFinal mac (.getBytes client-id))]
    (.encodeToString (Base64/getEncoder) raw-hmac)))

(calculate-secret-hash
  {:client-id "client-id"
   :client-secret "client-secret"
   :username "username"})

(comment

  (require '[cognitect.aws.client.api :as aws])

  (def cognito-idp (aws/client {:api :cognito-idp}))

  (keys (aws/ops cognito-idp))

  (aws/doc cognito-idp :AdminInitiateAuth)

  (aws/validate-requests cognito-idp true)

  (let [client-id (-> cr/system :auth :config :client-id)
        client-secret (-> cr/system :auth :config :client-secret)
        user-pool-id (-> cr/system :auth :config :user-pool-id)
        cognito-idp (-> cr/system :auth :cognito-idp)
        email "jacek.schae+aws-test@gmail.com"]
    (aws/invoke cognito-idp
      {:op :AdminInitiateAuth
       :request
       {:ClientId client-id
        :UserPoolId user-pool-id
        :AuthFlow "ADMIN_USER_PASSWORD_AUTH"
        :AuthParameters {"USERNAME" email
                         "PASSWORD" "Pa$$w0rd"
                         "SECRET_HASH" (calculate-secret-hash
                                         {:client-id client-id
                                          :client-secret client-secret
                                          :username email})}}}))

  ;
  ;{:UserConfirmed false,
  ; :CodeDeliveryDetails {:Destination "j***@g***", :DeliveryMedium "EMAIL", :AttributeName "email"},
  ; :UserSub "f7526f17-fced-4246-9b79-12499891c681"}

  ;{:__type "UsernameExistsException",
  ; :message "An account with the given email already exists.",
  ; :cognitect.anomalies/category :cognitect.anomalies/incorrect}


  (-> (transit-write {:name "name"
                      :public true
                      :prep-time 30
                      :img "https://github.com/clojure.png"})
    (transit-read))


  (let [db (d/db (-> cr/system :database :conn))
        recipe-pattern [:recipe/recipe-id
                        :recipe/prep-time
                        :recipe/display-name
                        :recipe/image-url
                        :recipe/public?
                        :account/_favorite-recipes
                        {:recipe/owner
                         [:account/account-id
                          :account/display-name]}
                        {:recipe/steps
                         [:step/step-id
                          :step/description
                          :step/sort-order]}
                        {:recipe/ingredients
                         [:ingredient/ingredient-id
                          :ingredient/display-name
                          :ingredient/amount
                          :ingredient/measure
                          :ingredient/sort-order]}]]
    (mapv query-result->recipe (d/q '[:find (pull ?e pattern)
                                      :in $ pattern
                                      :where [?e :recipe/public? false]]
                                 db recipe-pattern)))



  (pt/response-for
    (-> cr/system :api-server :service ::http/service-fn)
    :get "/recipes"
    :headers {"Authorization" "auth|5fbf7db6271d5e0076903601"
              "Content-Type" "application/transit+json"})

  (pt/response-for
    (-> cr/system :api-server :service ::http/service-fn)
    :post "/recipes"
    :headers {"Authorization" "auth|5fbf7db6271d5e0076903601"
              "Content-Type" "application/transit+json"}
    :body (transit-write {:name "name"
                          :public true
                          :prep-time 30
                          :img "https://github.com/clojure.png"}))


  (let [conversation-pattern [:conversation/conversation-id
                              {:conversation/messages
                               [:message/message-id
                                :message/body
                                {:message/owner
                                 [:account/account-id
                                  :account/display-name]}]}]]
    (d/q '[:find (pull ?c pattern)
           :in $ ?account-id pattern
           :where
           [?a :account/account-id ?account-id]
           [?c :conversation/participants ?a]]
      (d/db (-> cr/system :database :conn)) "auth|5fbf7db6271d5e0076903601" conversation-pattern))


  (d/q '[:find ?e ?id
         :where [?e :account/account-id ?id]]
       (d/db (-> cr/system :database :conn)))


  (let [db (d/db (-> cr/system :database :conn))
        conversation-id #uuid"362d06c7-2702-4273-bcc3-0c04d2753b6f"
        message-pattern [:message/message-id
                         :message/body
                         :message/created-at
                         {:message/owner
                          [:account/account-id
                           :account/display-name]}]]
    (->> (d/q '[:find (pull ?m pattern)
                :in $ ?conversation-id pattern
                :where
                [?e :conversation/conversation-id ?conversation-id]
                [?e :conversation/messages ?m]]
           db conversation-id message-pattern)
      (map first)
      (sort-by :message/created-at)))

  (let [db (d/db (-> cr/system :database :conn))
        conversation-id #uuid"362d06c7-2702-4273-bcc3-0c04d2753b6f"
        account-id "auth|5fbf7db6271d5e0076903601"]
    (->> (d/q '[:find ?m
                :in $ ?conversation-id ?account-id
                :where
                [?a :account/account-id ?account-id]
                [?c :conversation/conversation-id ?conversation-id]
                [?c :conversation/participants ?a]
                [?c :conversation/messages ?m]
                (not [?m :message/read-by ?a])]
           db conversation-id account-id)
      (map first)))

  (d/transact
    (-> cr/system :database :conn)
    {:tx-data [[:db/add 92358976733305 :message/read-by [:account/account-id "auth|5fbf7db6271d5e0076903601"]]]})

  (defn cheffy-interceptors
    [service-map sys-interceptors]
    (let [default-interceptors (-> service-map
                                   (http/default-interceptors)
                                   ::http/interceptors)
          interceptors (into [] (concat
                                  (butlast default-interceptors)
                                  sys-interceptors
                                  [(last default-interceptors)]))]
      (assoc service-map ::http/interceptors interceptors)))

  (let [conversation-id (random-uuid)
        from "auth|5fbf7db6271d5e0076903601"
        to "mike@mailinator.com"
        message-body "message body"
        message-id (random-uuid)]
    (d/transact
      (-> cr/system :database :conn)
      {:tx-data [{:conversation/conversation-id conversation-id
                  :conversation/participants (mapv #(vector :account/account-id %) [to from])
                  :conversation/messages (str message-id)}
                 {:db/id (str message-id)
                  :message/message-id message-id
                  :message/owner [:account/account-id from]
                  :message/read-by [[:account/account-id from]]
                  :message/body message-body
                  :message/created-at (Date.)}]}))


  (start-dev)

  (restart-dev)

  (stop-dev)

  )