(ns rehiring.job-loader
  (:require
    [clojure.walk :as walk]
    [rehiring.db :as rhdb]
    [rehiring.subs :as subs]
    [re-frame.core :as rfr]
    [cljs.pprint :as pp]
    [clojure.string :as str]))

;; --- loading job data -----------------------------------------

(defn monthlies-kw []
  (walk/keywordize-keys (js->clj js/gMonthlies)))

(rfr/reg-event-db
  :month-set
  (fn [db [_ hn-id]]
    (assoc db :month-hn-id hn-id)))

(defn pick-a-month []
  (let [months (monthlies-kw)]
    [:div {:class "pickAMonth"}
     [:select {:class     "searchMonth"
               :value     (:hnId (nth months rhdb/SEARCH-MO-STARTING-IDX))
               :on-change (fn [e]
                            (let [opt (.-target e)
                                  hnid (.-value opt)]
                              (println "Hello onchange" (js->clj opt) hnid)
                              (rfr/dispatch [:month-set hnid])))}
      (let []
        (map (fn [mno mo-def]
               (let [{:keys [hnId desc] :as all} mo-def]
                 [:option {:key hnId :value hnId}
                  desc]))
          (range)
          months))]]))

(declare mk-page-loader jobs-collect)

(defn job-listing-loader []
  (fn []
    [:div {:style {:display "none"}}
     (let [selId @(rfr/subscribe [:month-hn-id])
           moDef (some (fn [mo]
                         (when (= (:hnId mo) selId)
                           mo))
                   (monthlies-kw))]
       (assert moDef)
       ;;(println :modef (:pgCount moDef) moDef)

       (if (pos? (:pgCount moDef))
         (doall (map (fn [pgn]
                       ^{:key (str selId "-" (inc pgn))} [mk-page-loader selId (inc pgn)])
                  (range (:pgCount moDef))))
         [mk-page-loader selId]))]))

(rfr/reg-event-db
  :month-page-collect
  (fn [db [_ ifr-dom hn-id pg-no]]
    ;(println :replacing-jobs hn-id pg-no)
    (assoc db :jobs (jobs-collect ifr-dom))))

(defn mk-page-loader []
  (fn [hn-id pg-no]
    (let [src-url (pp/cl-format nil "files/~a/~a.html" hn-id (or pg-no hn-id))]
      (println :mkpg hn-id pg-no src-url)
      [:iframe {:src     src-url
                :on-load #(let [ifr (.-target %)]
                            ;;(println "Loaded!!" ifr hn-id pg-no src-url)
                            (rfr/dispatch [:month-page-collect ifr hn-id pg-no]))}])))

(declare job-spec job-spec-extend)

(defn jobs-collect [ifr-dom]
  (if-let [cont-doc (.-contentDocument ifr-dom)]
    (let [hn-body (aget (.getElementsByTagName cont-doc "body") 0)]
      (let [things (take 100 (prim-seq (.querySelectorAll hn-body ".athing")))]
        (println :things (count things))
        (let [jobs (filter #(:OK %) (map job-spec things))]
          (set! (.-innerHTML hn-body) (str (take 2 jobs)))
          ;;(println :j3 (take 10 jobs))
          jobs)))
    []))

(defn job-spec [dom]
  ;;(println "jobid!" (.-id dom) dom)
  (let [spec (atom {:hn-id (.-id dom)})]
    (doseq [child (prim-seq (.-children dom))]
      (job-spec-extend spec child))
    (when (:OK @spec)
      ;;(println :fini (dissoc @spec :body :body-search))
      @spec)))

(def internOK (js/RegExp. "internship|intern" "i"))
(def nointernOK (js/RegExp. "no internship|no intern" "i"))
(def visaOK (js/RegExp. "visa|visas" "i"))
(def novisaOK (js/RegExp. "no visa|no visas" "i"))
(def onsiteOK (js/RegExp. "on.?site" "i"))
(def remoteOK (js/RegExp. "remote" "i"))
(def noremoteOK (js/RegExp. "no remote" "i"))

(defn job-spec-extend [spec dom]
  (let [cn (.-className dom)]
    (when (some #{cn} ["c5a" "cae" "c00" "c9c" "cdd" "c73" "c88"])
      (when-let [rs (.getElementsByClassName dom "reply")]
        (map (fn [e] (.remove e)) (prim-seq rs)))
      (let [child (.-childNodes dom)
            c0 (aget child 0)]

        ;; pre-digest all nodes
        (swap! spec assoc :body [])                         ;; needed?
        (if (and (= 3 (.-nodeType c0))
                 (< 1 (count (filter #{\|} (.-textContent c0)))))

          (let [s (atom {:in-header true
                         :title-seg []})]
            (doseq [n (prim-seq child)]
              (if (:in-header @s)
                (if (and (= 1 (.-nodeType n))
                         (= "P" (.-nodeName n)))
                  (do
                    (swap! s assoc :in-header false)
                    (swap! spec update-in [:body] conj n))
                  (swap! s update-in [:title-seg] conj n))
                (swap! spec update-in [:body] conj n)))

            (let [htext (str/join " | "
                          (map (fn [h] (.-textContent h)) (:title-seg @s)))
                  hseg (map str/trim (str/split htext #"\|"))
                  hsmatch (fn [rx]
                            (not (nil?
                                   (some (fn [h] (.match h rx)) hseg))))]
              ;(println :htext!!! htext)
              ;(println :hseg hseg )
              (swap! spec assoc :OK true)
              (swap! spec assoc :company (nth hseg 0))
              (swap! spec assoc :title-search htext)
              (swap! spec assoc :body-search
                (str/join "*4*2*"
                  (map (fn [n] (.-textContent n)) (:body @spec))))

              (swap! spec assoc :remote (and (hsmatch remoteOK)
                                             (not (hsmatch noremoteOK))))
              (swap! spec assoc :visa (and (hsmatch visaOK)
                                           (not (hsmatch novisaOK))))
              (swap! spec assoc :intern (and (hsmatch internOK)
                                             (not (hsmatch nointernOK))))
              (swap! spec assoc :onsite (hsmatch onsiteOK)))))))

    ;; always fall through, but do not descend into replies
    (when (not= cn "reply")
      (doseq [child (prim-seq (.-children dom))]
        (job-spec-extend spec child)))))

