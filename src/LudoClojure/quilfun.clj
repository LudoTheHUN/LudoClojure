(ns LudoClojure.quilfun
 ; (:use quil.core))
(:use quil.core)
(:use [LudoClojure.pperceptron]))

;TODO
;lib for displaying pp's in quil
  ;TestHelpers, build out functions for loading on training pairs, easily and in other libs (eg: here).
  ;Colour each output of :vecProductResult_buf
  ;display answers vs correct answers to see errors globally
  ;parameter space over 2D

(def pp0 (make_pp {:input_size 3
                   :outputs_size 2
                   :pp_size 6
                   :rho 2               ;;  accuracy to which pp should learn, 1 means give back a binary 1,-1 output, 2means 1,0,-1, assuming pp is of an odd size etc.
                   :eta (float 0.01)    ;;  learning_rate
                   :gama (float 0.1)    ;;  margin around zero              ;0.4
                   :epsilon (float 0.1) ;;  level of error that is allowed.
                   :mu (float 0.2 )}))  ;;  learning modifier around zero   ;0.9

(def frmecounter (atom 0))
(def xpoint (atom 0))


(pp_train_and_answer pp0 [1.0 0.0 -1.0] [-1.0 1.0])
(pp_readout pp0 :vecProductResult_buf)

(defn createcolours [pp] 
      (let [pp_size (count (pp_readout pp :vecProductResult_buf))]
       (range 0 pp_size)
  ))
(createcolours pp0)

(defn draw_pp_vec_points [pp]
  (let [pp_vec (pp_readout pp :alpha_buf)]
   (dorun (map (fn [x] (do 
                      (stroke 0 11 255)
                      (point @xpoint (+ (/ (* x (height)) 4) (/ (height) 2) ))))
            pp_vec))))

(def pp_answers [
                 [[-1.0 -1.0 -1.0]   [-1.0  1.0]]
                 [[ 1.0 -1.0 -1.0]   [ 1.0 -1.0]]
                 [[ 1.0  1.0 -1.0]   [-1.0  1.0]]
                 [[-1.0  1.0 -1.0]   [ 1.0 -1.0]]
                 [[ 0.0  0.0 -1.0]   [ 1.0  1.0]]
                 [[ 0.0  1.0 -1.0]   [ 0.0  1.0]]
                 [[ 0.0 -1.0 -1.0]   [ 1.0  0.0]]
                 [[ 1.0  0.0 -1.0]   [ 0.0  1.0]]
                  ])




(defn learnthis []
 (let [picker  (random (count pp_answers))
       q  (first (nth pp_answers picker))
       a  (second (nth pp_answers picker))]
   ;(println  q a)
  (pp_train_and_answer pp0 q a {:gama (float 0.05)})  
)) ; (learnthis)

(defn all_answers []  "ask all questions, get all answers, show them side by side")
  
(pp_answer pp0 [-1.0 -1.0 -1.0] )
(pp_answer pp0 [-1.0  1.0 -1.0] ) ;[ 1.0 -1.0]
(pp_answer pp0 [0.9  0.4 -1.0] )
(pp_answer pp0 [0.0  1.0 -1.0] )  ;[-1.0 1.0]
(pp_readout pp0 :vecProductResult_buf)
(pp_readout pp0 :alpha_buf)


(defn setup []
  (smooth)                          ;;Turn on anti-aliasing
  (frame-rate 100)                    ;;Set framerate to 1 FPS
  (background 100))                 ;;Set the background colour to
                                    ;;  a nice shade of grey.
       ;;Draw a circle at x y with the correct diameter



(defn update_xpoint! [] (swap! xpoint (fn [x] (if (> x (width))
                        0
                        (inc x)
                         ))))

(def xpoints (atom [0 1 12 13 14]))
(swap! xpoints (fn [_] [0 1 12 13 102236]))

(defn clearscreen []
  (if (= @xpoint 0)
  (background 100)))

(defn draw_xpoints []
   (fill 250 0 50)
   (stroke 255 0 0)
   (rect @xpoint (/ (+ (random (height)) (* 0.5 (height))) 2)  3 5))

(defn write_out_text []
   (fill 250 0 50)
   (rect 0 0 100 10)
   (fill 255)
   (text (str @frmecounter) 10 10 )
   (text (str @xpoint) 70 10 ))

(defn draw []
  (clearscreen)
  (stroke 255 0 0)
  ;(stroke (random 255) (random 255) (random 255)) ;;Set the stroke colour to a random grey

  ; (point (random (width)) (random (height)))
 ; (doall (map (fn [x] (text (str "fo" x) (random 255) (random 255) )) (range 0 100)))
 ;(map (fn [x] (point (random (width) (random (height))))) (range 0 10))
  ; (dorun (map (fn [_] (point (random (width)) (random (height)))) (range 0 1000)))  ;shimmer background
   (swap! frmecounter inc)
   (learnthis)
   (update_xpoint!)
   ;(draw_xpoints)
   (write_out_text)
   (draw_pp_vec_points pp0)
  )

(defsketch example                  ;;Define a new sketch named example
  :title "Oh so many grey points"  ;;Set the title of the sketch
  :setup setup                      ;;Specify the setup fn
  :draw draw                        ;;Specify the draw fn
  :size [323 200])                  ;;You struggle to beat the golden ratio











;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;vv
(comment
  
(defn draw []
  (stroke (random 255))             ;;Set the stroke colour to a random grey
  
  (stroke-weight (random 10))       ;;Set the stroke thickness randomly
  (fill (random 255))               ;;Set the fill colour to a random grey

  (let [diam (random 100)           ;;Set the diameter to a value between 0 and 100
        x    (random (width))       ;;Set the x coord randomly within the sketch
        y    (random (height))]     ;;Set the y coord randomly within the sketch
    (ellipse x y diam diam))
  (text "fooo")
  )


(defn setup []
  (smooth)
  (background 230 230 230)
  (stroke 130, 0 0)
  (stroke-weight 4)
  (let [cross-size      70
        circ-size       50
        canvas-x-center (/ (width) 2)
        canvas-y-center (/ (height) 2)
        left            (- canvas-x-center cross-size)
        right           (+ canvas-x-center cross-size)
        top             (+ canvas-y-center cross-size)
        bottom          (- canvas-y-center cross-size)]
    (line left bottom right top)
    (line right bottom left top)

    (fill 255 150)
    (ellipse canvas-x-center canvas-y-center circ-size circ-size)))

(defsketch gen-art-1
  :title "Cross with circle"
  :setup setup
  :size [500 300])



(defn setup []
  (frame-rate 24)
  (smooth)
  (background 180)
  (stroke 0)
  (stroke-weight 5)
  (fill 255 25)
  (let [diams (range-incl 10 400 10)]
    (set-state! :diam (seq->stream diams)
                :cent-x (/ (width) 2)
                :cent-y (/ (height) 2))))

(defn draw []
  (let [cent-x (state :cent-x)
        cent-y (state :cent-y)
        diam   ((state :diam))]
    (when diam
      (background 180)
      (ellipse cent-x cent-y diam diam))))

(defsketch gen-art-2
  :title "Growing circle"
  :setup setup
  :draw draw
  :size [500 300]
  :keep-on-top true)



(defn draw-line
  "Draws a horizontal line on the canvas at height h"
  [h]
  (stroke 0 (- 255 h))
  (line 10 h (- (width) 20) h)
  (stroke 255 h)
  (line 10 (+ h 4) (- (width) 20) (+ h 4)))

(defn setup []
  (background 180)
  (stroke-weight 4)
  (stroke-cap :square)
  (let [line-heights (range 10 (- (height) 15) 10)]
    (dorun (map draw-line line-heights))))

(defsketch example-4
  :title "Fading Horizontal Lines"
  :setup setup
  :size [500 300])


(ns quil.examples.gen-art.09-sine-wave-with-noise
  (:use quil.core
        [quil.helpers.drawing :only [line-join-points]]
        [quil.helpers.seqs :only [range-incl]]
        [quil.helpers.calc :only [mul-add]]))


(defn setup []
  (smooth)
  (sphere-detail 100)
  (translate (/ (width) 2) (/ (height) 2) 0)
  (sphere 100)
  )

(defsketch gen-art-26
  :title "3D Sphere"
  :setup setup
  :size [500 300]
  :renderer :opengl)


(defn setup []
  (smooth)
  (sphere-detail 100)
  (translate (/ (width) 2) (/ (height) 2) 0)
  (sphere 100))

(defsketch gen-art-26
  :title "3D Sphere"
  :setup setup
  :size [500 300]
  :renderer :p3d)

)