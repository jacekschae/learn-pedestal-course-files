(ns cheffy.components.auth
  (:require [com.stuartsierra.component :as component]
            [cognitect.aws.client.api :as aws]
            [com.stuartsierra.component.repl :as cr]
            [clojure.data.json :as json])
  (:import (javax.crypto.spec SecretKeySpec)
           (javax.crypto Mac)
           (java.util Base64)
           (com.auth0.jwk UrlJwkProvider GuavaCachedJwkProvider)
           (com.auth0.jwt.interfaces RSAKeyProvider)
           (com.auth0.jwt.algorithms Algorithm)
           (com.auth0.jwt JWT)))

(def token

  {:ChallengeParameters {},
   :AuthenticationResult {:AccessToken "eyJraWQiOiI5TkxiWVJIcDVtM042d1VUZVFsNlM4WnlXN0haQWtvZXR6ODd5NFVtTmk4PSIsImFsZyI6IlJTMjU2In0.eyJzdWIiOiJmNzUyNmYxNy1mY2VkLTQyNDYtOWI3OS0xMjQ5OTg5MWM2ODEiLCJpc3MiOiJodHRwczpcL1wvY29nbml0by1pZHAudXMtZWFzdC0xLmFtYXpvbmF3cy5jb21cL3VzLWVhc3QtMV9oaHZKelJRdE0iLCJjbGllbnRfaWQiOiIxOTVqZGFqM2piNXZjMmEyMjhiMjAyNmNoaCIsIm9yaWdpbl9qdGkiOiI3MmVhYzc0NC1hNjYxLTQ1MTktYWRhMC1kYTY1NWJlMmFmNzYiLCJldmVudF9pZCI6IjZiN2IwYmU3LWFjNzgtNDA3OS1hYTIwLTM0YjliODVhZWJiOCIsInRva2VuX3VzZSI6ImFjY2VzcyIsInNjb3BlIjoiYXdzLmNvZ25pdG8uc2lnbmluLnVzZXIuYWRtaW4iLCJhdXRoX3RpbWUiOjE2NjI1NTA3ODcsImV4cCI6MTY2MjU1NDM4NywiaWF0IjoxNjYyNTUwNzg3LCJqdGkiOiIxZjk3ZDJmMy1jMWE1LTQwYWYtOWI1NS1lMWNlODlmMWY2OGIiLCJ1c2VybmFtZSI6ImY3NTI2ZjE3LWZjZWQtNDI0Ni05Yjc5LTEyNDk5ODkxYzY4MSJ9.OgYVhiagsNoTzkDiP6SJdXQtaT8n3VPs9lAW3UTr32zaejJbK0m0Z11DdduKpJQr5RFFqMZveGvp7ZJ2rRv5nDrNnobQ9yrfzdX0Rx476W2m7OnHjQ0DqutE29d3_nTxlVrDrnB2FL8k11bjeFn4PifVTbVJv9ClUn-M_n7a7IHAzmrZbkgZ55AzJYe2_tlrGzvBeddSmUZPUSWsmsWJfViooLjVPhse5rJr_Z4ePNB9acBUJ7aGMRIO5hk3ylofZk8vWKUf5FF3hP9NPa_siDuAa03tBK2Imv0rJ8iiDALlHBi-0yJyTifGWRALTXo51DxpJjDj2alzT3HO3c4YpA",
                          :ExpiresIn 3600,
                          :TokenType "Bearer",
                          :RefreshToken "eyJjdHkiOiJKV1QiLCJlbmMiOiJBMjU2R0NNIiwiYWxnIjoiUlNBLU9BRVAifQ.bG87r84bwbyd-kkUHx47Ib44WySYxPW2K8ekkj7iJswQm9OixXCND96o7nyBhAEE4oO2jjJe__Tnu7k7rPU2MF_P73f1lYQSz-bIJRz70RqORDKqytCxmtocbbXUez2SRpG4E7YPPr4arE9q-YCS-cQyODSRKEwUaZNvQEse9xifhWkdQ_rGPPIt56OARmytjhAiXkDcCjtPm6vBqBAcGo04Y7_wZA6thQiTpWOrMRUokiskhuX79oFJI9Dr20nGlxstNHbwc8EZ8uFZNHng4RcsktBufiKvqv17Wp-bPBkhAhaTK4iB7J19NR4vtsuNRaRL8eKtVI1JQINDH8fSqQ.i-B_WJeHevgQ3ZAC.681YHQTPFo4CSKuiR00C9-6IGSsY9EkZmG_QI056kX5TswKQPqPdroh0zYeTHVCNqPDkaWC1Ur6vxnzvkx1bvF8Lac-6h_xfF5MKzjFsIVHQRZEL2QGvIFEhW1RbIKofei8w-KpljfwL10Tprv2TJsuiF46qP86utnODvk40diO7v2IWAu5gd5lyHD5oqFhzWBsnRRgGd2FRmlho2vmXqaqemny3_QtsL6YD0aC5fl-izKkcZrzttkeZEmdZFab30lSh_UrEiiXrxU1pDuEqlpTpGgLA0h1NDh-xvRhEapbo_KPWjYo1gKPEus6NsmDO6D_3nLJ-R24q9cnv0NLBIb4Lb-mFfw1vxnodhdqlE3KyGlour58g0cqUgtqWlnUgZGDERpZvhZMlMhOBIOaRcHjKKJdzCOsmTI8ym4Fx--ed9o-IeQTp9KQMHgR0hiQnLnP-aBo5Mn7T3KnoUaZmHYR09UQOCvQ5PNjBqOZLunevFVptYM-EYE7363m9er7aTFkR3u5uYlSIzM3liV0OHD49tHJ8A6mLpWMG5TL3awUrYfTXo4HaCnb1mWNR0nNgEwq-l65KrlVdI79mk8hL10_hiIBKb-9TynDGnGPB-8ZkFOgboxOEZX46gLKUtJdpiLABp_QpOUWG6K4LRK5fcH740MWCheJ4xJYaKIwbWZWcL5TeQOBmOkoFbAzFjmpf_mVB6rjUk5XgHkVkQWHA8biIvvcNoKj90R-k9ky83U8OfW1VHeBB54e5tUfOJCR2-o8pjMAclTlvNfPcHnF0qQ5Fysu7K6BpXpPmOVX42HOqBjJOiQMN6u-3hA22xeEqu0RO9Lv64Yf7cpkI86HNShcK9Hw9_-QcgdyNWf1W7jnoMurnN3B4xSfa7L_yw7LOV8TzFt1GswrTWEhVGAV-XOZreYxfIEkhMgtbTMt_zBWhr9z1aRoWlN4MAKSSLFA6uY8qijH484-JTGAI3qdlpjQWNQRa-8MVbB0EJhg77O01FKgBZYHa4gQ5rHt3M7IrmvBiBMF0_206gy3-kFkqml-ZwWjYP2EcjeSw3REr1ImtgUKbFYKxNFCU2v-PfY_n3jrgEDsSqMSCbW-g7OJF5xe27pXbwchWHzAPCWjHjNSuB6x0jWDWqXfLDHKGoF0B9EJOcMTYAp76ZdU-Xc0dEoOy0ko3no3p7_soKG79UndGlWxeYXLlUnMBMFPU5HKiWu-voFYVFMnLW7pSPW6gvXotuCOWrRln4Qxr1Eb5CsDZBDv0XwxTnRoZLurrBJ5TRvgSwnCkzHz-dIqpiqv7GzuG0n04-DIa_s8w12Kewj4qEyU9J47iPcV_rA.U-gVfb8Usv4TOcH5EAMJFw",
                          :IdToken "eyJraWQiOiJIdUV5NXFPOE5hakRhOEhOaERQelFGek52SGNQK0NIR2F1MldlV1wvVnNUQT0iLCJhbGciOiJSUzI1NiJ9.eyJzdWIiOiJmNzUyNmYxNy1mY2VkLTQyNDYtOWI3OS0xMjQ5OTg5MWM2ODEiLCJlbWFpbF92ZXJpZmllZCI6dHJ1ZSwiaXNzIjoiaHR0cHM6XC9cL2NvZ25pdG8taWRwLnVzLWVhc3QtMS5hbWF6b25hd3MuY29tXC91cy1lYXN0LTFfaGh2SnpSUXRNIiwiY29nbml0bzp1c2VybmFtZSI6ImY3NTI2ZjE3LWZjZWQtNDI0Ni05Yjc5LTEyNDk5ODkxYzY4MSIsIm9yaWdpbl9qdGkiOiI3MmVhYzc0NC1hNjYxLTQ1MTktYWRhMC1kYTY1NWJlMmFmNzYiLCJhdWQiOiIxOTVqZGFqM2piNXZjMmEyMjhiMjAyNmNoaCIsImV2ZW50X2lkIjoiNmI3YjBiZTctYWM3OC00MDc5LWFhMjAtMzRiOWI4NWFlYmI4IiwidG9rZW5fdXNlIjoiaWQiLCJhdXRoX3RpbWUiOjE2NjI1NTA3ODcsImV4cCI6MTY2MjU1NDM4NywiaWF0IjoxNjYyNTUwNzg3LCJqdGkiOiJiZjNlYjFlOS05YjQ2LTRhNDctODY5Mi1kMDMyMDBmZTY2YWYiLCJlbWFpbCI6ImphY2VrLnNjaGFlK2F3cy10ZXN0QGdtYWlsLmNvbSJ9.NdUEwjzslwKw5f2_Iq73IDbJequVAk9XRuWjNtZ672nZyalA7dnO__HOz4dk3Tr53PWCzZjC_o7MqVsPtt-59XLsM7QpF_6rFvx4xq-qpDoOmX2VlNZrzMdKL2XV7ZFvuXNXjiUGpok0TNxXbCZJtiSwyYVoXMetmE6Ah7GY7v-9l1FEhB6JbB2abBHtZvRrCqBx1DFfuXg7kuEnU9_BKBEv4LtTpL7X-AwCFbLZLw_LU0t1vXnayFlBkqpjWXTFRPx2T4r-XQpQUSiLc98xwETgAGdSCi_z011nYst9cGwDhrZsKMdBNU4VSWa4MkbQ6EQejB9SbWDs3AlCvtIsKg"}}


  )

(defn validate-signature
  [{:keys [key-provider]} token]
  (let [algorithm (Algorithm/RSA256 key-provider)
        verifier (.build (JWT/require algorithm))
        verified-token (.verify verifier token)]
    (.getPayload verified-token)))

(defn decode-to-str
  [s]
  (String. (.decode (Base64/getUrlDecoder) s)))

(defn decode-token
  [token]
  (-> token
    (decode-to-str)
    (json/read-str)))

(defn verify-payload
  [{:keys [config]} {:strs [client_id iss token_use] :as payload}]
  (when-not
    (and
      (= (:client-id config) client_id)
      (= (:jwks config) iss)
      (contains? #{"access" "id"} token_use)
      )
    (throw (ex-info "Token verification failed" {})))
  payload)

(defn verify-and-get-payload
  [auth token]
  (->> token
    (validate-signature auth)
    (decode-token)
    (verify-payload auth)))

(comment

  (verify-and-get-payload
    (:auth cr/system)
    (-> token :AuthenticationResult :AccessToken))

  )

(defn calculate-secret-hash
  [{:keys [client-id client-secret username]}]
  (let [hmac-sha256-algorithm "HmacSHA256"
        signing-key (SecretKeySpec. (.getBytes client-secret) hmac-sha256-algorithm)
        mac (doto (Mac/getInstance hmac-sha256-algorithm)
              (.init signing-key)
              (.update (.getBytes username)))
        raw-hmac (.doFinal mac (.getBytes client-id))]
    (.encodeToString (Base64/getEncoder) raw-hmac)))

(defn when-anomaly-throw
  [result]
  (when (contains? result :cognitect.anomalies/category)
    (throw (ex-info (:__type result) result))))

(defn create-cognito-account
  [{:keys [config cognito-idp]} {:keys [email password]}]
  (let [client-id (:client-id config)
        client-secret (:client-secret config)
        result (aws/invoke cognito-idp
                 {:op :SignUp
                  :request
                  {:ClientId client-id
                   :Username email
                   :Password password
                   :SecretHash (calculate-secret-hash
                                 {:client-id client-id
                                  :client-secret client-secret
                                  :username email})}})]
    (when-anomaly-throw result)
    [{:account/account-id (:UserSub result)
      :account/display-name email}]))

(defn confirm-cognito-account
  [{:keys [config cognito-idp]} {:keys [email confirmation-code]}]
  (let [client-id (:client-id config)
        client-secret (:client-secret config)
        result (aws/invoke cognito-idp
                 {:op :ConfirmSignUp
                  :request
                  {:ClientId client-id
                   :Username email
                   :ConfirmationCode confirmation-code
                   :SecretHash (calculate-secret-hash
                                 {:client-id client-id
                                  :client-secret client-secret
                                  :username email})}})]
    (when-anomaly-throw result)))

(defn cognito-log-in
  [{:keys [config cognito-idp]} {:keys [email password]}]
  (let [client-id (:client-id config)
        client-secret (:client-secret config)
        user-pool-id (:user-pool-id config)
        result (aws/invoke cognito-idp
                 {:op :AdminInitiateAuth
                  :request
                  {:ClientId client-id
                   :UserPoolId user-pool-id
                   :AuthFlow "ADMIN_USER_PASSWORD_AUTH"
                   :AuthParameters {"USERNAME" email
                                    "PASSWORD" password
                                    "SECRET_HASH" (calculate-secret-hash
                                                    {:client-id client-id
                                                     :client-secret client-secret
                                                     :username email})}}})]
    (when-anomaly-throw result)
    (:AuthenticationResult result)))

(defrecord Auth [config cognito-idp key-provider]

  component/Lifecycle

  (start [component]
    (println ";; Starting Auth")
    (let [key-provider (-> (:jwks config)
                         (UrlJwkProvider.)
                         (GuavaCachedJwkProvider.))]
      (assoc component
        :cognito-idp (aws/client {:api :cognito-idp})
        :key-provider (reify RSAKeyProvider
                        (getPublicKeyById [_ kid]
                          (.getPublicKey (.get key-provider kid)))

                        (getPrivateKey [_]
                          nil)

                        (getPrivateKeyId [_]
                          nil)))))

  (stop [component]
    (println ";; Stopping Auth")
    (assoc component
      :cognito-idp nil
      :key-provider nil)))

(defn service
  [config]
  (map->Auth {:config config}))