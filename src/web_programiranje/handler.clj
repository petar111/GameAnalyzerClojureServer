(ns web-programiranje.handler
  (:require [compojure.core :refer :all]
            [compojure.route :as route]
            [ring.middleware.defaults :refer [wrap-defaults site-defaults api-defaults]]
            [cheshire.core :as chjson]
            [ring.middleware.json :as ring-json]
            [ring.util.response :as ring-response]
            [ring.middleware.cors :as ring-cors]
            [web-programiranje.service.game-service :as game-service]
            [web-programiranje.service.auth-service :as auth-service]
            [web-programiranje.security.jwt-token-provider :as jwt-token-provider]
            ))

(defn make-response
  "docstring"
  [response-dto]
  (->(ring-response/response (chjson/generate-string response-dto))
     )
  )

(defn make-response-login [dto-response jwt-token]
  (-> (make-response dto-response)
      (ring-response/header "Jwt-token" jwt-token)
      )
  )




(defn handle-login [login-request]
  (let [login-signal (auth-service/login login-request)]
    (if (= (:signal login-signal) "LOGIN_SUCCESS")
      (let [jwt-token (jwt-token-provider/generate-jwt-token (assoc jwt-token-provider/claim :sub (:username (:user login-signal))))]
        (make-response-login (:user login-signal) jwt-token)
        )
      (make-response login-signal)
      )
    )
  )

(defroutes app-routes
           (GET "/" request (str request))
           (GET "/test" [] (ring-response/response {:ana "ana" :bara 1}))
           (GET "/user/:username" [username] (make-response (auth-service/get-user-by-username username)))
           (GET "/game/:id{[0-9]+}" [id] (make-response (game-service/get-game-by-id id)))
           (GET "/game/all" [] (make-response (game-service/get-all-games)))
           (POST "/login" request (handle-login (:body request)))
           (route/not-found "Not Found")
           )

(def app
  (-> app-routes
      (wrap-defaults api-defaults)
      (ring-json/wrap-json-response)
      (ring-json/wrap-json-body {:keywords? true :bigdecimals? true})
      (ring-cors/wrap-cors :access-control-allow-origin #"http://localhost:4200" :access-control-expose-headers ["Jwt-token"] :access-control-allow-methods [:get :post])
      )
  )
