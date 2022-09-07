(ns cheffy.components.auth
  (:require [com.stuartsierra.component :as component]
            [cognitect.aws.client.api :as aws]
            [com.stuartsierra.component.repl :as cr])
  (:import (javax.crypto.spec SecretKeySpec)
           (javax.crypto Mac)
           (java.util Base64)
           (com.auth0.jwk UrlJwkProvider GuavaCachedJwkProvider)
           (com.auth0.jwt.interfaces RSAKeyProvider)
           (com.auth0.jwt.algorithms Algorithm)
           (com.auth0.jwt JWT)))

(def token

  {:ChallengeParameters {},
   :AuthenticationResult {:AccessToken "eyJraWQiOiI5TkxiWVJIcDVtM042d1VUZVFsNlM4WnlXN0haQWtvZXR6ODd5NFVtTmk4PSIsImFsZyI6IlJTMjU2In0.eyJzdWIiOiJmNzUyNmYxNy1mY2VkLTQyNDYtOWI3OS0xMjQ5OTg5MWM2ODEiLCJpc3MiOiJodHRwczpcL1wvY29nbml0by1pZHAudXMtZWFzdC0xLmFtYXpvbmF3cy5jb21cL3VzLWVhc3QtMV9oaHZKelJRdE0iLCJjbGllbnRfaWQiOiIxOTVqZGFqM2piNXZjMmEyMjhiMjAyNmNoaCIsIm9yaWdpbl9qdGkiOiI4YjIxOGFlNi1lYmYzLTQ5MmYtOGY3NC1iN2EyMTlmNzYwOTciLCJldmVudF9pZCI6ImRlNmNmZTBhLTY5MmMtNGU5Ny05MGM3LTlkYjUxOTZkYjM4OSIsInRva2VuX3VzZSI6ImFjY2VzcyIsInNjb3BlIjoiYXdzLmNvZ25pdG8uc2lnbmluLnVzZXIuYWRtaW4iLCJhdXRoX3RpbWUiOjE2NjI1NDY4MDEsImV4cCI6MTY2MjU1MDQwMSwiaWF0IjoxNjYyNTQ2ODAxLCJqdGkiOiJkOGUwMTU4My00ZTI5LTQ1MmMtYTY2Zi0yZDRhMWY5OGZiZDUiLCJ1c2VybmFtZSI6ImY3NTI2ZjE3LWZjZWQtNDI0Ni05Yjc5LTEyNDk5ODkxYzY4MSJ9.mZ_kDgFj7omINmFd6CRgZmC9S7iNiJkE7nhrDQZ9tNQGZXYKtRjUXqZ8etPmj4A9u9_lPLsAZMG4Wz_YYwJu2rD0ngLu_cgr6UuTQyLWm1PihLXnOMyRklMQ9De7VWS9gHO_WaMNE7ahFZHEn2XASqP3ERFPW2VJEa2mevW80BDvEf1j6XaK6yg-aA3jFpnW3KPGaeWIkkwEKjYgWYQKGfzG5jdOcYkHooztGf-mr_UoO5jRUrBLv7Z0RwlHgtdmSu9jxc-GAGQizgX9oZoNBsprU4-gOQzneLIbnePMnTyvvm8bVUfNwBKGyDdtErl36gnXMXfc_G4vhuhQKVcDDw",
                          :ExpiresIn 3600,
                          :TokenType "Bearer",
                          :RefreshToken "eyJjdHkiOiJKV1QiLCJlbmMiOiJBMjU2R0NNIiwiYWxnIjoiUlNBLU9BRVAifQ.SSqVynuhuImHlsM0CnREV9v2uxVivPDi_taC8mShQlBfxAO5CxnUHZeB3xgklBweChVCw3EZd-awDDSnmwTHFe-7d4S04bRCCr6ko1BUTgzk0RZfMFcdws6A-pqmecwXfwMZnT9g1rDJ47Ymjq7nkkHWtK8O_lffbd-U3Ai9KXzPn7PjNMfk7v9bWAdpq4bS6NJ2hWjb2_x4NY7joZ9vkhWVNslzfiQhSnYfeJlweBLHNnE4UFiVMC2vHfS9sMdSw4zM5jogZWcEAbmloEDtNHJ7RL_3gMGUCv1Iwl1Ut2uXn21Cbu3h03S2Kd0dPKiZqGe-FuxN0Z8Bw8dVpCtZLw.vHMp6cafa7G6l7Fs.qXVh3GsrPh2ICedWInIYbfCnMPo9iC6Y2IvSr299jiAQ2wTWMSDFAwmq9fXfgSvIfpOdRSZiEXMqOw_CGe6G1eOQy3wNQOeb0KNkqdy1oXIsV7OCn8___tCPdxNntXrhS-lMH4nJQZ9eRynXUBTmBxUtFuyNL0I8JCpmSAihJgcV3Ql4mIyNmcGSG5f6LTXAMqm1kOiaftt8BUPhTbwcfJLX6IZQ4P1p_6asK61kSH7vQSOgziL6gPVecejzmd4Ry9q8kP8NUpEtXOgXQV6RPRMkMgdpdJAfS3n0ruepxKQEYG4il2TbmNmzHTqKYuKFHBosjL0kNhX7pMd8XblHdx1NPiglu191H9CYve3JDM6AfBUroLMctzyz7NTnbT-B6i2OpZbaDiYEyTBI-eAqBWWQz5CsMTr6d7nG3dql8OnfHCga40gmvagxhiIGQOeehzBQLaDrKDzBfsdSPKR8nvhCqLMnOm4gzm6Ok1yazcoAYzamlAJTsrsJ1sL6P68S60bTYzxQjHqJR_5QHoMYz3VVv8ilXevbiGPtiL1GxwzmIOKo3sVv_Iztvzumg8aBfrt_hKKBpJKBvz4TlOTmxV38rBBXZyRjjxURz_wwC2tO3OguBewLX3ZE_TgoWtI9XeXXX5se5eKiRdVGGYQ1L87t5_V5RQu-b2Bc9yyIdLcvp_cLJLZ19yQ-wyHQ-PkqH7WMoTsrDZgGFs5CRvDfSG-amVsemkXTdsQicxIXqG61PeTbXVn_smykvdEyNjybsbTgCgZcwy6gTo8CrFS2jogwKmJChdpv5izTOlzJe1J_oSrG5EkZf4VVad40SFXp1vwanNkJwZMBr6pI9DjUEFhpWb3dStAt5VEhNTKh33zsNKJmiOFtQMdAkT6gOIRS9BYqOtc5ikRw_9b75jk7sIhmY8WLD8rnQAAUDYn3Z8_q1i800LgsRMM2TBUwO8cRIxvt4krSEU0Kqzit43Q-jnesQvUZzHq6J5BmTDj_wGESxQXaD835tnSlh-yeZX85GFoRnHmvuH7GDYu6aLbBqfSf0xI_QRlbcLl9iHVJ3kv8qjhgxFcwjxPlZckzpE8-b-AqiXlmWpoBUuBBew-INcK7RnMQZkeg_D9K0O9pH1ooKxe5pz-KzkyKONSWtOrZkA-9QqTJc3zc6OyszgITlltt2sIwnXzvO9xN8aBScPsvbtnIvPtMAdIKzCRtTH3S8VOBPcpwKPi4B72GoZOKlyvQ3vSBfoaZpu21fSV8rmDrSfAJuabPylriIkPHJv93W6byn2BgbXyioW2ooHO4rYZkGUKZ-WyzilfF-TCyNgZQ29q82x3U1Hw-QQ.lADXS59flids90HaSJshWw",
                          :IdToken "eyJraWQiOiJIdUV5NXFPOE5hakRhOEhOaERQelFGek52SGNQK0NIR2F1MldlV1wvVnNUQT0iLCJhbGciOiJSUzI1NiJ9.eyJzdWIiOiJmNzUyNmYxNy1mY2VkLTQyNDYtOWI3OS0xMjQ5OTg5MWM2ODEiLCJlbWFpbF92ZXJpZmllZCI6dHJ1ZSwiaXNzIjoiaHR0cHM6XC9cL2NvZ25pdG8taWRwLnVzLWVhc3QtMS5hbWF6b25hd3MuY29tXC91cy1lYXN0LTFfaGh2SnpSUXRNIiwiY29nbml0bzp1c2VybmFtZSI6ImY3NTI2ZjE3LWZjZWQtNDI0Ni05Yjc5LTEyNDk5ODkxYzY4MSIsIm9yaWdpbl9qdGkiOiI4YjIxOGFlNi1lYmYzLTQ5MmYtOGY3NC1iN2EyMTlmNzYwOTciLCJhdWQiOiIxOTVqZGFqM2piNXZjMmEyMjhiMjAyNmNoaCIsImV2ZW50X2lkIjoiZGU2Y2ZlMGEtNjkyYy00ZTk3LTkwYzctOWRiNTE5NmRiMzg5IiwidG9rZW5fdXNlIjoiaWQiLCJhdXRoX3RpbWUiOjE2NjI1NDY4MDEsImV4cCI6MTY2MjU1MDQwMSwiaWF0IjoxNjYyNTQ2ODAxLCJqdGkiOiJjOGYwYTdiNS04ZGE3LTQzNWQtYjg3Ni0yYTZlYzAyMjJiOGQiLCJlbWFpbCI6ImphY2VrLnNjaGFlK2F3cy10ZXN0QGdtYWlsLmNvbSJ9.RGta9NOqXI3Xkg6lidZBBwwdKGiDDBZxuc9FSLOWWq0HWbMO_tON1D4QSyr8cKfclSpRPAhWt6ay1bK6aFmmPGZSHs28Xv6ka0Y16Y9wmkZDExNwMA45hkejTL_IFw1vz1E5SlhmwuWeHi1pQyNQ1kk_u5jXz8TB5SmuZuGDrPaUWrD0g61c2pE16fKSKOy4XRFE97pkNrX1eZ40o7JudsvMoqQH7x-E-96C9o5nMwtyHQKo8ZC0KZpiVQCdPRvfOH1nZ9XGFo-NfngmY-zblO_Zfd7hPUSAp-oZtfg9bWdO-u53G3H8HsTg3-ZTXfpet0_zPMn3_6YO0ECDcjCAMA"}}

  )

(defn validate-signature
  [{:keys [key-provider]} token]
  (let [algorithm (Algorithm/RSA256 key-provider)
        verifier (.build (JWT/require algorithm))
        decoded-token (.verify verifier token)]
    (.getPayload decoded-token)))

(comment

  (validate-signature
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