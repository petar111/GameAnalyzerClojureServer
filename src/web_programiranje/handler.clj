(ns web-programiranje.handler
  (:require [compojure.core :refer :all]
            [compojure.route :as route]
            [ring.middleware.defaults :refer [wrap-defaults site-defaults api-defaults]]
            [cheshire.core :as chjson]
            [ring.middleware.json :as ring-json]
            [ring.util.response :as ring-response]
            [web-programiranje.service.game-service :as game-service]
            [web-programiranje.service.auth-service :as auth-service]
            ))

(defn make-response
  "docstring"
  [response-dto]
  (->(ring-response/response (chjson/generate-string response-dto))
     (ring-response/header  "Content-type" "application/json")
     )
  )

(defroutes app-routes
           (GET "/" request (str request))
           (GET "/test" [] (ring-response/response {:ana "ana" :bara 1}))
           (GET "/user/:username" [username] (make-response (auth-service/get-user-by-username username)))
           (GET "/game/:id{[0-9]+}" [id] (make-response (game-service/get-game-by-id id)))
           (GET "/game/all" [] (make-response (game-service/get-all-games)))
           (POST "/login" request (make-response (auth-service/login (:body request))))
           (route/not-found "Not Found")
           )

(def app
  (-> app-routes
      (wrap-defaults api-defaults)
      (ring-json/wrap-json-response)
      (ring-json/wrap-json-body)
      )
  )
