(ns ^:figwheel-always icfp2015-vis.core
  (:require-macros [cljs.core.async.macros :refer [go go-loop]])
  (:require [goog.dom :as dom]
            [om.core :as om :include-macros true]
            [om-tools.core :refer-macros [defcomponent]]
            [om-tools.dom :as d :include-macros true]
            [chord.client :refer [ws-ch]]
            [cljs.core.async :refer [<! >! put! close!]]))

(enable-console-print!)

(defn create-board [width height & filled]
  {:width width
   :height height
   :filled filled})

(defonce app-state
  (atom {:board (create-board 10 10)}))

(defn server-loop [app-state]
  (go
    (println "Open Web Socket ...")
    (let [{:keys [ws-channel error]} (<! (ws-ch "ws://localhost:5011"))]
      (if-not error
        (go-loop []
          (when-let [{:keys [message]} (<! ws-channel)]
            (om/update! app-state :board message)
            (recur)))
        (println "Error:" (pr-str error))))))

(defn hex [x y state]
  (d/div {:class (str "hex " (name state))}
    (d/span {:class "shape"} \u2B22)
    (d/span {:class "text"} (str x ", " y))))

(defn filled? [board x y]
  (if (some #{[x y]} (:filled board)) :full :empty))

(defn row [board y]
  (apply d/div {:class "row"}
    (for [x (range (:width board))]
      (hex x y (filled? board x y)))))

(defn pixel-board-width [board]
  (+ 35 (* 60 (:width board))))

(defcomponent board [board]
  (render [_]
    (apply d/div {:class "board" :style {:width (pixel-board-width board)}}
      (for [y (range (:height board))]
        (row board y)))))

(defcomponent app [app-state owner]
  (will-mount [_]
    (server-loop app-state))
  (render [_]
    (om/build board (:board app-state))))

(om/root app app-state
  {:target (dom/getElement "app")})

(defn on-js-reload []
  ;; optionally touch your app-state to force rerendering depending on
  ;; your application
  ;; (swap! app-state update-in [:__figwheel_counter] inc)
  )
