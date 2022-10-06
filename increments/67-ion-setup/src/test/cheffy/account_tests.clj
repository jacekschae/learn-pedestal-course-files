(ns cheffy.account-tests
  (:require [clojure.test :refer :all]
            [com.stuartsierra.component.repl :as cr]
            [io.pedestal.test :as pt]
            [io.pedestal.http :as http]
            [cheffy.test-system :as ts]))

(def service-fn (-> cr/system :api-server :service ::http/service-fn))

(def random-email (atom (str "jacek.schae+" (random-uuid) "@gmail.com")))

(def tokens (atom nil))

(deftest account-tests
  (testing "sing-up"
    (let [{:keys [status body]} (-> (pt/response-for
                                      service-fn
                                      :post "/account/sign-up"
                                      :headers {"Content-Type" "application/transit+json"}
                                      :body (ts/transit-write {:email @random-email
                                                               :password "Pa$$w0rd"}))
                                  (update :body ts/transit-read))]
      (is (= 200 status))
      (is (contains? body :account-id))))

  (testing "confirm"
    (let [{:keys [status]} (pt/response-for
                                  service-fn
                                  :post "/account/confirm"
                                  :headers {"Content-Type" "application/transit+json"}
                                  :body (ts/transit-write {:email @random-email
                                                           :confirmation-code "763118"}))]
      (is (= 204 status))))

  (testing "log-in"
    (let [{:keys [status body]} (-> (pt/response-for
                                      service-fn
                                      :post "/account/log-in"
                                      :headers {"Content-Type" "application/transit+json"}
                                      :body (ts/transit-write {:email @random-email
                                                               :password "Pa$$w0rd"}))
                                  (update :body ts/transit-read))]
      (reset! tokens body)
      (is (= 200 status))))

  (testing "refresh"
    (let [{:keys [status body]} (-> (pt/response-for
                                      service-fn
                                      :post "/account/refresh"
                                      :headers {"Authorization" (str "Bearer " (:AccessToken @tokens))
                                                "Content-Type" "application/transit+json"}
                                      :body (ts/transit-write {:refresh-token (:RefreshToken @tokens)}))
                                  (update :body ts/transit-read))]
      (reset! tokens body)
      (is (= 200 status))))

  (testing "update-role"
    (let [{:keys [status body]} (pt/response-for
                                  service-fn
                                  :put "/account"
                                  :headers {"Authorization" (str "Bearer " (:AccessToken @tokens))
                                            "Content-Type" "application/transit+json"})]
      (is (= 200 status))))

  (testing "delete"
    (let [{:keys [status body]} (pt/response-for
                                  service-fn
                                  :delete "/account"
                                  :headers {"Authorization" (str "Bearer " (:AccessToken @tokens))
                                            "Content-Type" "application/transit+json"})]
      (is (= 200 status)))))