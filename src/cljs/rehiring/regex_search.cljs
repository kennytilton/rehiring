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

;function rebuildRgxTree( mx) {
;
;      let matchCase = mx.fmUp("rgxMatchCase").value
;, cvtOrAnd = mx.fmUp("rgxOrAnd").value
;, search =  cvtOrAnd? mx.rgxRaw.replace(/\sor\s/," || ").replace(/\sand\s/," && ") : mx.rgxRaw;
;
;      clg("building from search str", search);
;
;
;  mx.rgxTree = search.split('||').map(orx => orx.trim().split('&&').map(andx => {
;           try {
;                let [term, options=''] = andx.trim().split(',')
;                                                            if ( !matchCase && options.search('i') === -1)
;                                                            options = options + 'i'
;                                                            return new RegExp( term, options)
;                }
;           catch (error) {
;                          alert(error.toString() + ": <" + andx.trim() + ">")
;                          }
;           }))
;}





;function buildRgxTree(mx, e) {
;mx.rgxRaw = e.target.value.trim()
;
;if (mx.rgxRaw === '') {
;           mx.rgxTree = null // test
;           } else {
;                   if (mx.history.indexOf( mx.rgxRaw) === -1) {
;                                                               //clg('adding to rgx!!!!', mx.rgxRaw)
;                                                               mx.history = mx.history.concat(mx.rgxRaw)
;                                                               }
;                      rebuildRgxTree(mx)
;                   }
;}

(defn mk-listing-rgx [prop label desc]
  [:div {:style {:display        "flex"
                 :flex-direction "column"
                 :margin         "6px 18px 0 30px"}}
   [:span {:style {:color     "white"
                   :font-size "0.7em"}}
    label]
   [:input {:placeholder (pp/cl-format nil "Regex for ~a search" desc)
            :list        (str prop "list")
            :on-blur     #(let [rgx-raw (str/trim (.-value (.-target %)))]
                            (println :rgx!!!!!!!! prop rgx-raw)
                            (rfr/dispatch [:rgx-unparsed-set prop rgx-raw]))
            ;; todo on keypress
            :on-focus    #(.setSelectionRange (.-target %) 0 999)
            :value       (when (= prop :title)
                           "Crowd")
            :on-change   #(happy?)
            :style       {:min-width "72px"
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
      (println :rgx-normal rgx-normal match-case)
      (when rgx-normal
        (map (fn [or-term]
               (println :or-term or-term)
               (map (fn [and-term]
                      (println :and-term and-term)
                      (let [[term options] (str/split (str/trim and-term) ",")]
                        (println :newrgx and-term term options)
                        (let [rgx (js/RegExp. term (if (and (not match-case)
                                                            (not (str/includes? options ",")))
                                                     (str options "i")))]
                          (println :truergx rgx))))
                 (str/split or-term #"&&")))
          (str/split rgx-normal #"\|\|"))))))


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
