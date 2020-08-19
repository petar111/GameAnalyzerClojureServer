(ns web-programiranje.service.game-service
  (:require [web-programiranje.db.service.db-service :as db-service] ;includes applying database configuration
            [web-programiranje.mapper.mapper :as dto-mapper]))


(defn get-all-games
  "docstring"
  []
  (dto-mapper/to-game-info-list-dto
    (db-service/get-all-games))
  )

(defn get-game-by-id
  "docstring"
  [id]
  (dto-mapper/to-game-dto
    (db-service/get-game-by-id id))
  )

(defn insert [game]
  (let [saved-game (db-service/execute-transaction (db-service/insert-game game))]
    (if (empty? (:saved-players saved-game))
      {:message (str "Game is NOT saved")}
      {:message (str "Game " (:name saved-game) " is saved")}
      )
    )
  )

(defn get-by-name [name]
  (dto-mapper/to-game-dto (db-service/find-game-by-name name))
  )

(defn get-game-session-by-id [id]
  (dto-mapper/to-game-session-dto (db-service/get-game-session-by-id id))
  )

(defn insert-game-session [game-session]
  (let [saved-game-session (db-service/execute-transaction (db-service/insert-game-session game-session))]
    (if (empty? (:saved_players saved-game-session))
      {:message (str "Error. Game session not saved")}
      (dto-mapper/to-game-session-dto (db-service/get-game-session-by-id (:id saved-game-session)))
      )
    )
  )

(defn update-game-session [game-session]
  (let [saved-game-session (db-service/execute-transaction (db-service/update-game-session game-session))]
    (if saved-game-session
      (dto-mapper/to-game-session-dto (db-service/get-game-session-by-id (:id game-session)))
      {:message (str "Error. Game session not saved")}
      )
    )
  )

(defn save-game-session [game-session]
  (if (nil? (:id game-session))
    (insert-game-session game-session)
    (update-game-session game-session)
    )
  )