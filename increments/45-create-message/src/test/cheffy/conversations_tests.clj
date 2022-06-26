(ns cheffy.conversations-tests
  (:require [clojure.test :refer :all]
            [io.pedestal.test :as pt]
            [io.pedestal.http :as http]
            [cheffy.test-system :as ts]
            [com.stuartsierra.component.repl :as cr]))


(def service-fn (-> cr/system :api-server :service ::http/service-fn))

(deftest conversation-tests

  (testing "conversation tests"
    (let [{:keys [status body]} (-> (pt/response-for
                                      service-fn
                                      :get "/conversations"
                                      :headers {"Authorization" "auth|5fbf7db6271d5e0076903601"})
                                  (update :body ts/transit-read))]
      (is (= 200 status)))))