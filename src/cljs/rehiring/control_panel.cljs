(ns rehiring.control-panel
  (:require [rehiring.filtering :as flt]
            [rehiring.utility :as utl]
            [rehiring.job-listing-control-bar :as jlcb]
            [re-frame.core :as rfr]))

(defn control-panel []
  (fn []
    [:div {:style {:background "#ffb57d"}}
     ;[open-shut-case "os-filters" "Filters" true flt/mk-title-selects]
     [flt/mk-title-selects]
     [flt/mk-user-selects]
     [jlcb/job-listing-control-bar]]))
