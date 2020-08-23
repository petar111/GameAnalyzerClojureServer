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
            [web-programiranje.service.user-service :as user-service]
            [web-programiranje.security.jwt-token-provider :as jwt-token-provider]
            ))



(def cors-exposed-headers ["Origin" "Content-Type" "Accept" "Jwt-Token" "Authorization"
                           "Access-Control-Allow-Origin" "Access-Control-Allow-Credentials" "WWW-Authenticate"])
(def cors-allowed-methods [:get :post])

(defn make-response
  "docstring"
  [response-dto]
  (->(ring-response/response (chjson/generate-string response-dto))
     )
  )

(defn make-response-with-authentication [dto-response jwt-token]
  (-> (make-response dto-response)
      (ring-response/header "Jwt-token" jwt-token)
      )
  )



(defn make-error-response [exception status]
  (-> (ring-response/response (.getMessage exception))
      (ring-response/header "WWW-Authenticate" "Jwt-token realm=\"Application for game analyzing\", charset=\"UTF-8\"")
      (ring-response/status status))
  )



(defn handle-login [login-request]
  (try
    (let [login-signal (auth-service/login login-request)]
      (let [jwt-token (jwt-token-provider/generate-jwt-token (assoc jwt-token-provider/claim :sub (:username (:user login-signal))))]
        (make-response-with-authentication (:user login-signal) jwt-token)
        )
      )
    (catch Exception e (make-error-response e 401))
    )
  )

(defn handle-register
  "docstring"
  [user]
  (try
    (let [register-signal (auth-service/register-user user)]
      (let [jwt-token (jwt-token-provider/generate-jwt-token (assoc jwt-token-provider/claim :sub (:username (:user register-signal))))]
        (make-response-with-authentication (:user register-signal) jwt-token)
        )
      )
    (catch Exception e (make-error-response e 401))
    )
  )



(defn handle-request
  "docstring"
  [handle]
  (try
    (make-response (handle))
    (catch Exception e (make-error-response e 400))
    )
  )


(defroutes app-routes
           (GET "/" request (str request))
           (GET "/user/:id" [id] (handle-request (partial user-service/get-user-by-id id)))
           (GET "/user/:id/followers" [id] (handle-request (partial user-service/get-user-followers-usernames-by-user-id id)))
           (GET "/user/:id/following" [id] (handle-request (partial user-service/get-user-following-usernames-by-user-id id)))
           (GET "/user/:id/games" [id] (handle-request (partial game-service/get-games-by-user-id id)))
           (POST "/register" request (handle-register (:body request)))
           (POST "/user/update" request (handle-request (partial auth-service/update-user (:body request))))
           (POST "/user/update/experience" request  (handle-request (partial user-service/update-user-experience (:user (:body request)) (:experience (:body request)))))
           (GET "/game/:id/advice" [id] (handle-request (partial game-service/get-game-advice-by-id id)))
           (POST "/game/:id/request-verification" request (handle-request (partial game-service/request-verification (:body request))))
           (GET "/game/:id{[0-9]+}" [id] (handle-request (partial game-service/get-game-by-id id)))
           (GET "/game/score/get" [user_id game_id] (handle-request (partial game-service/get-game-scores user_id game_id)))
           (POST "/game/score/submit" request (handle-request (partial game-service/insert-game-score (:body request))))
           (GET "/game/all" [] (handle-request (partial game-service/get-all-games)))
           (POST "/login" request (handle-login (:body request)))
           (POST "/game/insert" request (handle-request (partial game-service/insert (:body request))))
           (GET "/game/get" [name] (handle-request (partial game-service/get-by-name name)))
           (GET "/game/game-session/get-by-creator" [username] (handle-request (partial game-service/get-game-session-infos-by-creator-username username))) ;this one and one under are somehow in conflict if i put this one under
           (GET "/game/game-session/:id" [id] (handle-request (partial game-service/get-game-session-by-id id)))
           (POST "/game/game-session/save" request (handle-request (partial game-service/save-game-session (:body request))))
           (route/not-found "Not Found")
           )

(def app
  (-> app-routes
      (wrap-defaults api-defaults)
      (ring-json/wrap-json-response)
      (ring-json/wrap-json-body {:keywords? true :bigdecimals? true})
      (ring-cors/wrap-cors :access-control-allow-origin #"http://localhost:4200" :access-control-expose-headers cors-exposed-headers :access-control-allow-methods cors-allowed-methods)
      )
  )
