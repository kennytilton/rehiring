(ns rehiring.regex-search
  (:require [cljs.pprint :as pp]
            [re-frame.core :refer [subscribe] :as rfr]
            [rehiring.utility :as utl]
            [reagent.core :as rgt]
            [clojure.string :as str]))



(defn mk-rgx-match-case []
  (fn []
    [:div {:style {:color       "#fcfcfc"
                   :margin      "0 9px 0 0"
                   :display     "flex"
                   :flex-wrap   "wrap"
                   :align-items "center"}}
     [:input {:id        "rgxMatchCase"
              :type      "checkbox"
              :value     @(rfr/subscribe [:toggle-key :rgx-match-case])
              :on-change (fn [e]
                           (rfr/dispatch [:toggle-key :rgx-match-case]))}]
     [:label {:for "rgxMatchCase"}
      "match case"]]))


(defn mk-rgx-or-and []
  (fn []
    [:div {:style {:color       "#fcfcfc"
                   :margin      "0 9px 0 0"
                   :display     "flex"
                   :flex-wrap   "wrap"
                   :align-items "center"}}
     [:input {:id        "rgxOrAnd"
              :type      "checkbox"
              :checked   @(rfr/subscribe [:toggle-key :rgx-xlate-or-and])
              :title     "Replace 'or/and' with '||/&&' for easier mobile entry."
              :on-change (fn [e]
                           (rfr/dispatch [:toggle-key :rgx-xlate-or-and]))}]
     [:label {:for "rgxOrAnd"}
      "allow or/and"]]))

(defn help-toggle []
  [:p "help"])

(def regexHelpEntry
  (map identity
    ["Press <kbd style='background:cornsilk;font-size:1em'>Enter</kbd> or <kbd style='background:cornsilk;font-size:1em'>Tab</kbd> to activate, including after clearing.", (str "Separate <a href='https://developer.mozilla.org/en-US/docs/Web/JavaScript/Guide/Regular_Expressions'>JS RegExp-legal</a> terms with <b>||</b> or "
                                                                                                                                                                              "<b>&&</b> (higher priority) to combine expressions."), "'Allow or/and' option treats those as ||/&& for easier mobile entry.", "Regex terms are split on comma and passed to <b>new RegExp(pattern,flags)</b>.", "e.g. Enter <b>taipei,i</b> for case-insensitive search."]))

(defn mk-rgx-options []
  (let [helping (rgt/atom false)]
    (fn []
      ^{:key "opts"}
      [:div
       [:div {:style (merge utl/hz-flex-wrap-centered
                       {:padding-right "12px"
                        :margin        "4px 0 9px 30px"})}
        ^{:key "mcase"} [mk-rgx-match-case]
        ^{:key "andor"} [mk-rgx-or-and]
        ^{:key "help"} [:span {:style    {:color  "white" :margin-left "24px"
                                          :cursor "pointer"}
                               :on-click #(reset! helping (not @helping))}
                        "help"]
        ]
       [utl/help-list regexHelpEntry helping]])))

(defn happy? [])

(defn mk-listing-rgx [prop label desc]
  [:div {:style {:display        "flex"
                 :flex-direction "column"
                 :margin         "6px 18px 0 30px"}}
   [:span {:style {:color     "white"
                   :font-size "0.7em"}}
    label]
   [:input {:placeholder  (pp/cl-format nil "Regex for ~a search" desc)
            :list         (str prop "list")
            :on-key-press #(when (= "Enter" (js->clj (.-key %)))
                             (rfr/dispatch [:rgx-unparsed-set prop (str/trim (.-value (.-target %)))]))

            :on-blur      #(let [rgx-raw (str/trim (.-value (.-target %)))]
                             (println :rgx!!!!!!!! prop rgx-raw)
                             (rfr/dispatch [:rgx-unparsed-set prop rgx-raw]))
            ;; todo on keypress
            :on-focus     #(.setSelectionRange (.-target %) 0 999)
            :value        (if (= prop :title) "crowd,q" "")
            ;;:on-change   #(happy?)
            :style        {:min-width "72px"
                           :font-size "1em"
                           :height    "2em"}}]
   [:datalist {:id (str prop "list")}
    (map (fn [hs]
           [:option {:value hs}])
      @(rfr/subscribe [:search-history prop]))]])

(rfr/reg-event-db :rgx-unparsed-set
  (fn [db [_ scope raw]]
    (assoc-in db [:rgx-raw scope] raw)))

(rfr/reg-sub :rgx-raw
  (fn [db [_ scope]]
    (get-in db [:rgx-raw scope])))

(rfr/reg-sub :rgx-de-aliased
  ;; signal fn
  (fn [[_ scope] _]
    [(rfr/subscribe [:rgx-raw scope])
     (rfr/subscribe [:toggle-key :rgx-xlate-or-and])])

  ;; compute
  (fn [[rgx-raw xlate-or-and]]
    (println :de-alias-compute!! rgx-raw xlate-or-and)
    (when rgx-raw
      (if xlate-or-and
        (str/replace (str/replace rgx-raw #"\sand\s" " && ") #"\sor\s" " || ")
        rgx-raw))))

(rfr/reg-sub :rgx-tree
  ;; signal fn
  (fn [[_ scope] _]
    [(rfr/subscribe [:rgx-de-aliased scope])
     (rfr/subscribe [:toggle-key :rgx-match-case])])

  ;; compute
  (fn [signals]
    (println :sigs signals)
    (let [[rgx-normal match-case] signals]
      (println :rgx-normal rgx-normal (type rgx-normal) (type (js->clj rgx-normal)))
      (when rgx-normal
        (let [or-terms (str/split (js->clj rgx-normal) #"\|\|")]
          (println :or-terms or-terms (count or-terms))
          (into []
            (map (fn [or-term]
                   (println :or-term or-term)
                   (into []
                     (map (fn [and-term]
                            (println :and-term and-term)
                            (let [[term options] (str/split (str/trim and-term) ",")
                                  netopts (if (and (not match-case)
                                                   (not (str/includes? (or options "") "i")))
                                            (str options "i")
                                            "")]
                              (println :newrgx and-term term netopts options)
                              (try
                                (let [rgx (js/RegExp. term netopts)]
                                  (println :truergx!!!! rgx)
                                  rgx)
                                (catch js/Object ex
                                  (js/alert (str "Invalid regex: " rgx-normal))
                                  nil))))
                       (str/split or-term #"&&")))))
            or-terms))))))


(rfr/reg-sub :search-history
  (fn [db [_ prop]]
    ;;(println :sub-runs! hn-id (get-in db [:show-job-details hn-id]))
    (get-in db [:search-details prop])))

(rfr/reg-event-db :search-history-extend
  (fn [db [_ prop new-search]]
    (update-in db [:search-history prop] conj new-search)))

(defn mk-title-rgx []
  ^{:key "title"}
  [mk-listing-rgx :title "Title Only?" "title"])

(defn mk-full-rgx []
  ^{:key "full"}
  [mk-listing-rgx :full "Full Listing" "title and listing"])


(defn mk-regex-search []
  (fn []
    [:div
     [:span {:style {:margin-left "24px"}}
      "Search"]

     [:div {:class "osBody"
            :style {:background "#ff6600"}}
      [mk-title-rgx]
      [mk-full-rgx]
      [mk-rgx-options]]]))
