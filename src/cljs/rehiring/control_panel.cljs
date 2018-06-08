(ns rehiring.control-panel
  (:require [rehiring.filtering :as flt]))

(defn control-panel []
  (fn []
    [:div { :style {:background "#ffb57d"}}
      ;[open-shut-case "os-filters" "Filters" true flt/mk-title-selects]
     [flt/mk-title-selects]
     [flt/mk-user-selects]]))

