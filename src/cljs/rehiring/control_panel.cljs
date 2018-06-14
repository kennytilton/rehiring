(ns rehiring.control-panel
  (:require [rehiring.filtering :as flt]
            [rehiring.utility :as utl]
            [rehiring.job-listing-control-bar :as jlcb]
            [re-frame.core :as rfr]
            [rehiring.regex-search :as rgx]))



;;; --- sort bar -----------------------------------------------------------

(defn sort-bar []
  (fn []
    [:div {:style {:padding 0
                   :margin  "15px 0 0 24px"
                   :display "flex"}}
     [:span {:style {:margin-right "6px"}} "Sort by:"]
     [:ul {:style (merge utl/hz-flex-wrap
                    {:list-style "none"
                     :padding    0 :margin 0})}
      (let [curr-sort @(rfr/subscribe [:job-sort])]
        (map (fn [jsort]
             (let [{:keys [title]} jsort]
               ^{:key title}
               [:li
                [:button.sortOption
                 {:style    {:color (if (= title (:title curr-sort))
                                      "blue" "#222")}
                  :selected (= jsort curr-sort)
                  :on-click #(if (= title (:title curr-sort))
                               (rfr/dispatch [:job-sort-set (update curr-sort :order (fn [oo]
                                                                                       #_ (println :oo oo)
                                                                                       (* -1 oo)))])
                               (rfr/dispatch [:job-sort-set jsort]))}
                 (str (:title jsort) (if (= title (:title curr-sort))
                                       (if (= (:order curr-sort) -1)
                                         (utl/unesc "&#x2798") (utl/unesc "&#x279a"))))]]))
        utl/job-sorts))
      ]]))

;;; --- the beef -----------------------------------------------------

(defn control-panel []
  (fn []
    [:div {:style {:background "#ffb57d"}}
     [utl/open-shut-case :show-filters "Filters"
      flt/mk-title-selects
      flt/mk-user-selects]

     [rgx/mk-regex-search]

     [sort-bar]

     [jlcb/job-listing-control-bar]]))