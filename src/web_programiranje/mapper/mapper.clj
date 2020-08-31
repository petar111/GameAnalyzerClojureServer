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

(defn to-rank-dto
  "docstring"
  [rank]
  (dto-model/->rank (:id rank)
                    (:name rank)
                    (:experience_min rank)
                    (:experience_max rank))
  )

(defn to-user-dto
  "docstring"
  [user]
  (dto-model/->user (:id user)
                    (:first_name user)
                    (:last_name user)
                    (:username user)
                    (:email user)
                    nil
                    (:country user)
                    (:date_of_birth user)
                    (:is_account_non_locked user)
                    (:is_credentials_non_expired user)
                    (:is_enabled user)
                    (:is_account_non_expired user)
                    (:experience user)
                    (to-rank-dto (:rank user))
                    (:number_of_verified_games user)
                    )
  )

(defn to-user-list-dto
  "docstring"
  [users]
  (map #(to-user-dto %) users)
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
                    (:verification_status game)
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
                         (:verification_status game)
                    )
  )

(defn to-game-info-list-dto
  "docstring"
  [games]
  (map #(to-game-info-dto %) games)
  )


(defn to-game-session-player-strategy-dto
  "docstring"
  [game-session-player-strategy]
  (dto-model/->game-session-player-strategy (:id game-session-player-strategy)
                                            (:times_played game-session-player-strategy)
                                            (to-strategy-dto (:strategy game-session-player-strategy)))
  )


(defn to-game-session-player-strategy-list-dto
  "docstring"
  [strategies]
  (map #(to-game-session-player-strategy-dto %) strategies)
  )

(defn to-game-session-player-dto
  "docstring"
  [game-session-player]
  (dto-model/->game-session-player (:id game-session-player)
                                   (to-player-dto (:player game-session-player))
                                   (:total_payoff game-session-player)
                                   (to-strategy-dto (:selected_strategy game-session-player))
                                   (to-game-session-player-strategy-list-dto (:played_strategies game-session-player))
                                   (:player_label game-session-player))
  )

(defn to-game-session-player-list-dto
  "docstring"
  [players]
  (map #(to-game-session-player-dto %) players)
  )

(defn to-game-session-dto
  "docstring"
  [game-session]
  (dto-model/->game-session (:id game-session)
                            (to-user-dto (:user game-session))
                            (:number_of_rounds game-session)
                            (to-game-dto (:game game-session))
                            (to-game-session-player-list-dto (:players game-session))
                            )
  )

(defn to-game-session-info-dto
  "docstring"
  [game-session]
  (dto-model/->game-session-info (:id game-session)
                            (:number_of_rounds game-session)
                            (to-game-info-dto (:game game-session))
                            )
  )

(defn to-game-session-info-list-dto
  "docstring"
  [game-sessions]
  (map #(to-game-session-info-dto %) game-sessions)
  )

(defn to-game-advice-data-dto
  "docstring"
  [game-advice-data]
  (dto-model/->game-advice-data (:nash-equilbria game-advice-data))
  )

(defn to-game-score-dto
  "docstring"
  [game-score]
  (dto-model/->game-score (:id game-score)
                          (:total_payoff game-score)
                          (:number_of_rounds game-score)
                          (to-game-info-dto (:game game-score))
                          (to-user-dto (:user game-score))
                          (:date_created game-score)
                          )
  )

(defn to-game-score-list-dto
  "docstring"
  [game-scores]
  (map #(to-game-score-dto %) game-scores)
  )
