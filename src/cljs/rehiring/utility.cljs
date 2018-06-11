(ns rehiring.utility)

(defn slide-in-anime [ show?]
  (if show? "slideIn" "slideOut"))

(def hz-flex-wrap {:display "flex"
                   :flex-wrap "wrap"})

(def ls-key "rehiring-browser")                         ;; localstore key

(def hz-flex-wrap-centered
  {:display "flex"
   :flex-wrap "wrap"
   :align-items "center"})

(def hz-flex-wrap-bottom
  {:display "flex"
   :flex-wrap "wrap"
   :align-items "bottom"})