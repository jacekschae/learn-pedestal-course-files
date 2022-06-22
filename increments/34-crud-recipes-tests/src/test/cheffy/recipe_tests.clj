(ns cheffy.recipe-tests
  (:require [clojure.test :refer :all]
            [io.pedestal.test :as pt]
            [io.pedestal.http :as http]
            [com.stuartsierra.component.repl :as cr]
            [cheffy.test-system :as ts]))


(deftest recipe-tests

  (testing "list recipes"
    (testing "with auth -- public and drafts"
      (let [{:keys [status body]} (-> (pt/response-for
                                        (-> cr/system :api-server :service ::http/service-fn)
                                        :get "/recipes"
                                        :headers {"Authorization" "auth|5fbf7db6271d5e0076903601"})
                                    (update :body ts/transit-read))]
        (is (= 200 status))
        (is (vector? (:public body)))
        (is (vector? (:drafts body)))))

    (testing "without auth -- public"
      (let [{:keys [status body]} (-> (pt/response-for
                                        (-> cr/system :api-server :service ::http/service-fn)
                                        :get "/recipes")
                                    (update :body ts/transit-read))]
        (is (= 200 status))
        (is (vector? (:public body)))
        (is (nil? (:drafts body)))))))