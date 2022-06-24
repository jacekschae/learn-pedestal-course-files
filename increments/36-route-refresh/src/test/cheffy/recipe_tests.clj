(ns cheffy.recipe-tests
  (:require [clojure.test :refer :all]
            [io.pedestal.test :as pt]
            [io.pedestal.http :as http]
            [com.stuartsierra.component.repl :as cr]
            [cheffy.test-system :as ts]))


(def service-fn (-> cr/system :api-server :service ::http/service-fn))

(def recipe-id (atom nil))

(deftest recipe-tests

  (testing "list recipes"
    (testing "with auth -- public and drafts"
      (let [{:keys [status body]} (-> (pt/response-for
                                        service-fn
                                        :get "/recipes"
                                        :headers {"Authorization" "auth|5fbf7db6271d5e0076903601"})
                                    (update :body ts/transit-read))]
        (is (= 200 status))
        (is (vector? (:public body)))
        (is (vector? (:drafts body)))))

    (testing "without auth -- public"
      (let [{:keys [status body]} (-> (pt/response-for
                                        service-fn
                                        :get "/recipes")
                                    (update :body ts/transit-read))]
        (is (= 200 status))
        (is (vector? (:public body)))
        (is (nil? (:drafts body)))))

    (testing "create-recipe"
      (let [{:keys [status body]} (-> (pt/response-for
                                        service-fn
                                        :post "/recipes"
                                        :headers {"Authorization" "auth|5fbf7db6271d5e0076903601"
                                                  "Content-Type" "application/transit+json"}
                                        :body (ts/transit-write {:name "name"
                                                                 :public true
                                                                 :prep-time 30
                                                                 :img "https://github.com/clojure.png"}))
                                    (update :body ts/transit-read))]
        (reset! recipe-id (:recipe-id body))
        (is (= 201 status))))

    (testing "retrieve-recipe"
      (let [{:keys [status body]} (-> (pt/response-for
                                        service-fn
                                        :get (str "/recipes/" @recipe-id)
                                        :headers {"Authorization" "auth|5fbf7db6271d5e0076903601"
                                                  "Content-Type" "application/transit+json"})
                                    (update :body ts/transit-read))]
        (is (= 200 status))))


    (testing "update-recipe"
      (let [{:keys [status body]} (pt/response-for
                                    service-fn
                                    :put (str "/recipes/" @recipe-id)
                                    :headers {"Authorization" "auth|5fbf7db6271d5e0076903601"
                                              "Content-Type" "application/transit+json"}
                                    :body (ts/transit-write {:name "updated name"
                                                             :public true
                                                             :prep-time 30
                                                             :img "https://github.com/clojure.png"}))]
        (is (= 204 status))))

    (testing "delete-recipe"
      (let [{:keys [status body]} (pt/response-for
                                    service-fn
                                    :delete (str "/recipes/" @recipe-id)
                                    :headers {"Authorization" "auth|5fbf7db6271d5e0076903601"
                                              "Content-Type" "application/transit+json"})]
        (is (= 204 status))))))