(ns web-programiranje.mapper.mapper
  (:require [web-programiranje.model.model :as dto-model]))

(defn to-strategy-dto
  "docstring"
  [strategy]
  (dto-model/->strategy (:id strategy) (:name strategy))
  )

(defn to-strategies-list-dto [strategies]
  (map #(to-strategy-dto %) strategies)
  )


(defn to-payoff-dto
  "docstring"
  [payoff]
  (dto-model/->payoff (:id payoff)
                      (:amount payoff)
                      (to-strategy-dto (:strategy payoff))
                      (to-strategy-dto (:opposing_strategy payoff)))
  )

(defn to-payoff-list-dto [payoffs]
  (map #(to-payoff-dto %) payoffs)
  )

(defn extract-playable-strategies [payoffs]
  (to-strategies-list-dto (set (map #(:strategy %) payoffs)))
  )

(defn to-player-dto
  "docstring"
  [player]
  (dto-model/->player (:id player)
                      (:name player)
                      (to-payoff-list-dto (:payoffs player))
                      (extract-playable-strategies (:payoffs player))
                      )
  )

(defn to-player-list-dto
  "docstring"
  [players]
  (map #(to-player-dto %) players)
  )

(defn to-user-dto
  "docstring"
  [user]
  (dto-model/->user (:id user)
                    (:first_name user)
                    (:last_name user)
                    (:username user)
                    nil
                    (:country user)
                    (:date_of_birth user)
                    (:is_account_non_locked user)
                    (:is_credentials_non_expired user)
                    (:is_enabled user)
                    (:is_account_non_expired user)
                    )
  )


(defn to-game-dto
  "docstring"
  [game]
  (dto-model/->game (:id game)
                    (:name game)
                    (:external_info game)
                    (:description game)
                    (to-user-dto (:user game))
                    (to-strategies-list-dto (:strategies game))
                    (to-player-list-dto (:players game))
                    )
  )

(defn to-game-info-dto
  "docstring"
  [game]
  (dto-model/->game-info (:id game)
                         (:name game)
                         (:external_info game)
                         (:description game)
                         (:username (:user game))
                    )
  )

(defn to-game-info-list-dto
  "docstring"
  [games]
  (map #(to-game-info-dto %) games)
  )

