(ns cheffy.routes
  (:require [io.pedestal.http.route :as route]
            [cheffy.recipes :as recipes]
            [cheffy.recipes.steps :as steps]
            [cheffy.recipes.ingredients :as ingredients]
            [cheffy.conversations :as conversations]))


(defn routes
  []
  (route/expand-routes
    #{["/recipes" :get recipes/list-recipes :route-name :list-recipes]
      ["/recipes" :post recipes/create-recipe :route-name :create-recipe]
      ["/recipes/:recipe-id" :get recipes/retrieve-recipe :route-name :retrieve-recipe]
      ["/recipes/:recipe-id" :put recipes/update-recipe :route-name :update-recipe]
      ["/recipes/:recipe-id" :delete recipes/delete-recipe :route-name :delete-recipe]
      ;; steps
      ["/steps" :post steps/upsert-step :route-name :create-step]
      ["/steps/:step-id" :put steps/upsert-step :route-name :update-step]
      ["/steps/:step-id" :delete steps/delete-step :route-name :delete-step]
      ;; ingredients
      ["/ingredients" :post ingredients/upsert-ingredient :route-name :create-ingredient]
      ["/ingredients/:ingredient-id" :put ingredients/upsert-ingredient :route-name :update-ingredient]
      ["/ingredients/:ingredient-id" :delete ingredients/delete-ingredient :route-name :delete-ingredient]
      ;; conversations
      ["/conversations" :get conversations/list-conversations :route-name :list-conversations]
      ["/conversations" :post conversations/create-message :route-name :create-message-without-conversation]
      ["/conversations/:conversation-id" :post conversations/create-message :route-name :create-message-with-conversation]
      }))