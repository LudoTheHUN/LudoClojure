(ns LudoClojure.quilfun
 ; (:use quil.core))
(:use quil.core)
(:use [LudoClojure.pperceptron])
(:use [LudoClojure.liquid])
)  ;;TODO put down an as here

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
(def xpoint      (atom 0))
(def gama        (atom 0.05))


(pp_train_and_answer pp0 [1.0 0.0 -1.0] [-1.0 1.0])
(pp_readout pp0 :vecProductResult_buf)

(defn createcolours [pp] 
      (let [pp_size (count (pp_readout pp :vecProductResult_buf))]
       (range 0 pp_size)
  ))
(createcolours pp0)

(defn draw_pp_vec_points [pp]
  (let [pp_vec (pp_readout pp :alpha_buf)
        pp_vec_size (count pp_vec)]
   (dorun (map (fn [x col] (do 
                      (stroke (* 255 (/ col pp_vec_size))
                              (mod (* col 77) 255)
                              (mod (* col 33) 255))   ;;RAINBOWS!!!
                      (point @xpoint (+ (/ (* x (height)) 4) (/ (height) 2) ))))
            pp_vec (range pp_vec_size)))))

(map (fn [x y] (println x y)) [1 2 3] [5 6 7])


(def pp_answers [
                 [[-0.5  -0.5 -1.0]   [-1.0  1.0]]
                 [[ 0.5  -0.5 -1.0]   [-1.0  1.0]]
                 [[ 0.0   0.0 -1.0]   [-1.0  1.0]]
                 [[ 0.7   0.7 -1.0]   [-1.0  1.0]]
                 [[ 0.0   0.7 -1.0]   [-1.0  1.0]]
                 [[-0.5   0.5 -1.0]   [-1.0  1.0]]
                 [[ 0.0  -0.5 -1.0]   [ 1.0  1.0]]
                 
               
                 [[ 0.0  1.0 -1.0]   [ 1.0  1.0]]
                 [[ 0.0 -1.0 -1.0]   [ 1.0  1.0]]
                 [[ 1.0  0.0 -1.0]   [ 1.0  1.0]]
                 [[-1.0  0.0 -1.0]   [ 1.0  1.0]]
                 [[-1.0 -1.0 -1.0]   [ 1.0  1.0]]
                 [[ 1.0  1.0 -1.0]   [ 1.0  1.0]]
                 [[-1.0  1.0 -1.0]   [ 1.0  1.0]]
                 [[ 1.0 -1.0 -1.0]   [ 1.0  1.0]]
                  ])   ;Array of Input, correct answer training examples.




(def pp_answers [
                 [[-1.0 -1.0 -1.0]   [-1.0  1.0]]
                 [[ 1.0 -1.0 -1.0]   [-1.0 -1.0]]
                 [[ 1.0  1.0 -1.0]   [ 1.0  1.0]]
                 [[-1.0  1.0 -1.0]   [-1.0 -1.0]]
                 
                 [[ 0.0  0.0 -1.0]   [ 1.0  1.0]]
                 [[ 0.0  1.0 -1.0]   [ 1.0  1.0]]
                 [[ 0.0 -1.0 -1.0]   [-1.0  1.0]]
                 [[ 1.0  0.0 -1.0]   [ 1.0  1.0]]
                 [[-1.0  0.0 -1.0]   [ 1.0  1.0]]
                 [[ 0.7  0.6 -1.0]   [-1.0  1.0]]
                  ])   ;Array of Input, correct answer training examples.

;(pprint (all_answers pp_answers ))

(def pp_answers [
                 [[-0.5 -0.5 -1.0]   [ 1.0  1.0]]
                 [[ 0.5 -0.5 -1.0]   [-1.0 -1.0]]
                 [[-0.5  0.5 -1.0]   [-1.0 -1.0]]
                 [[ 0.5  0.5 -1.0]   [ 1.0  1.0]]
                  ])

(def pp_answers [
                 [[-0.5 -0.5 -1.0]   [-1.0  1.0]]
                 [[ 0.5 -0.5 -1.0]   [ 1.0 -1.0]]
                 [[-0.5  0.5 -1.0]   [ 1.0 -1.0]]
                 [[ 0.5  0.5 -1.0]   [-1.0  1.0]]
                  ])
;;(all_answers pp_answers)

(defn learnthis [a_val]
 (let [picker  (random (count pp_answers))
       q  (first (nth pp_answers picker))
       a  (second (nth pp_answers picker))]
   ;(println  q a)
  (pp_train_and_answer pp0 q a {:gama (float a_val) :mu (float a_val )})  
)) ; (learnthis)

(defn all_answers "ask all questions, get all answers, show them side by side
     (all_answers pp_answers )" 
  [pp_questions]
  (vec (map 
          (fn [question] 
            (let [answer (pp_answer pp0 (first question))]
            [(first question) (second question) answer (= (second question) answer)])) pp_questions))
  )

;(pprint (all_answers pp_answers ))

(defn create_float_range [size fraction] "list of floats
     (create_float_range 21 10)
     (count (create_float_range 41 20))
     eg: (map (fn[x] (float (/ (- x 10) 10))) (range 21))"
  (let [halfsize (int (/ size 2))]
   (map (fn[x] (float (/ (- x halfsize) fraction))) (range size)))
  )

;;(create_float_range 1001 500)

(defn all-pairs [sq] (for [i sq j sq] [i j]))   ;TGFSO

;;(create_float_range (create_float_range 1001 500))

(defn create_2d_pp_questions [size fraction]
  "(create_2d_pp_questions 5 2)"
  (let [float_list (create_float_range size fraction)
        pairs  (all-pairs float_list)]
    (map (fn [x] (vector (conj x -0.1) [0.0])) pairs)
    ;(reduce (fn [array afloat] (conj array float_list)) []  float_list)
  ))

;(all_answers (create_2d_pp_questions 5 2))
;(time (all_answers (create_2d_pp_questions 21 10)))
(def many_answers_done (time (all_answers (create_2d_pp_questions 11 5))))
;(count (create_2d_pp_questions 21 10))
;(count (create_2d_pp_questions 41 20))

(defn regularize_color 
  ([value]
    (regularize_color value -1 1))
  ([value min max]
    (let [colourval (* (- value min) (/ 255.0 (- max min) ))]
      (cond (> colourval 255.0) 255
            (< colourval 0.0) 0
            :else (int colourval)))))


(defn draw_answers [pp_answers]
(if ;(= (mod @xpoint 10) 1)
    true
  (let [q_and_As (all_answers 
                   ;(create_2d_pp_questions 201 100)
                   pp_answers
                   )]
    ;draw answers
  (dorun (map (fn [q_and_a] (do
               (let [col (regularize_color (first (nth q_and_a 2)))]
                (stroke col col col)
                (fill col col col)
                (rect (+ 
                        ;@xpoint 
                        200.0 (* 100.0 (first (first q_and_a)))) (+ (* 100.0 (second (first q_and_a))) 200.0) 8 8)
                ;(point (+ @xpoint 100.0 (* 100.0 (first (first q_and_a)))) (+ (* 100.0 (second (first q_and_a))) 200.0))
                )))
           q_and_As))
    ;draw correct ansers inside
  (dorun (map (fn [q_and_a] (do
               (let [col (regularize_color (first (nth q_and_a 1)))]
                (stroke col col col)
                (fill col col col)
                (rect (+ 
                        ;@xpoint 
                        200.0 (* 100.0 (first (first q_and_a)))) (+ (* 100.0 (second (first q_and_a))) 200.0) 4 4)
                ;(point (+ @xpoint 100.0 (* 100.0 (first (first q_and_a)))) (+ (* 100.0 (second (first q_and_a))) 200.0))
                )))
           q_and_As))
  
  )
  (do 
   ;             (stroke 255 0 0)
   ;             (fill 255 0 0)
   ; (point (+ @xpoint 100.0 (* 100.0 1.0)) (+ (* 100.0 1.0) 200.0))
    )
  ))



;;(draw_answers many_answers_done)

 ;(count (create_2d_pp_questions 41 20))
;(count (create_2d_pp_questions 101 50))
 ;(map (fn[x] (float (/ (- x 10) 10))) (range 21))
;(time (count (all_answers pp_answers)))
 
 ;  (count (filter true? (map (fn [x] (nth x 3)) (all_answers pp_answers))))
 
 (/ 4264 10201.0)
  
(pp_answer pp0 [-1.0 -1.0 -1.0] )
(pp_answer pp0 [-1.0  1.0 -1.0] ) ;[ 1.0 -1.0]
(pp_answer pp0 [0.9  0.4 -1.0] )
(pp_answer pp0 [0.0  1.0 -1.0] )  ;[-1.0 1.0]
(pp_readout pp0 :vecProductResult_buf)
(pp_readout pp0 :alpha_buf)


(defn update_xpoint! [] (swap! xpoint (fn [x] (if (> x (width))
                        0
                        (inc x)
                         ))))

(def xpoints (atom [0 1 12 13 14]))
(swap! xpoints (fn [_] [0 1 12 13 102236]))

(defn clearscreen []
  (if (= @xpoint 0)
  (background 100)))


(defn mouse_control_atom_x [an_atom]
   (if (= (mouse-state) true)
      (let [mouse_val (mouse-x)]
       (swap! an_atom (fn [_] (float (/ mouse_val 500)) )))
      nil))


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


(defn writoutstuff [txt x y width height]
   (fill 400 0 50)
   (rect x y width height)
   (fill 255)
   (text (str txt) (+ x 3) (+ 10 y) )
   )





; (def quil_liquid (make_liquid {:liquidsize (* 500) :connections 3}))   ;;interesting
(def quil_liquid (make_liquid {:liquidsize (* 798 ) :connections 2}))   ;;interesting

(defn drap_liquid [] 
   (do 
    (flop quil_liquid)
    ;(point @xpoint (random (height)))
   (let [liquid_state (readoff_speced quil_liquid [0 500])]
     (do ;(println liquid_state)
         (doall
           (map 
             (fn [x] 
                (do 
                    (let [liquidvalue (* (nth liquid_state x) 25)]
                      (stroke liquidvalue liquidvalue liquidvalue)
                      ;(point (- @xpoint 10) x)
                      ;(point 400 x)
                      (line (+ @xpoint 20) x 1900 x)
                      )))
             (range 500))))
    )
   ))




;; START QUIL DRAWING

(defn setup []
  (smooth)                          ;;Turn on anti-aliasing
  (frame-rate 1000)                    ;;Set framerate to 1 FPS
  (background 100))                 ;;Set the background colour to
                                    ;;  a nice shade of grey.
       ;;Draw a circle at x y with the correct diameter



(defn draw []
  (clearscreen)
  (stroke 255 0 0)
  ;(stroke (random 255) (random 255) (random 255)) ;;Set the stroke colour to a random grey
  ; (point (random (width)) (random (height)))
  ; (doall (map (fn [x] (text (str "fo" x) (random 255) (random 255) )) (range 0 100)))
  ;(map (fn [x] (point (random (width) (random (height))))) (range 0 10))
  ; (dorun (map (fn [_] (point (random (width)) (random (height)))) (range 0 1000)))  ;shimmer background
   (swap! frmecounter inc)
   (learnthis @gama)
   (update_xpoint!)
   ;(draw_xpoints)
   ;(write_out_text)
   (draw_pp_vec_points pp0)
   (mouse_control_atom_x gama)


   ;;liquid drawing
   (drap_liquid)
   
   (writoutstuff (str "@frmecounter " @frmecounter)      0 0  150 10)
   (writoutstuff (str "@xpoint" @xpoint)                 0 10 150 10)
   
   
   (writoutstuff (str "Acorr: " (count (filter true? (map (fn [x] (nth x 3)) (all_answers pp_answers)))))
                                         0 (- (height) 70) 150 10)
   (writoutstuff (str "Q's   : " (count pp_answers))
                                         0 (- (height) 60) 150 10)
   (writoutstuff (str "gama is: " @gama) 0 (- (height) 50) 150 10)
   (writoutstuff (mouse-state)           0 (- (height) 40) 150 10)
   (writoutstuff (mouse-y)               0 (- (height) 30) 150 10)
   (writoutstuff (mouse-x)               0 (- (height) 20) 150 10)
   (writoutstuff (mouse-button)          0 (- (height) 10) 150 10)
   
 ;  (draw_answers many_answers_done)
   
   (if (= (mod @xpoint 10) 1) (draw_answers (create_2d_pp_questions 11 5)))
   (draw_answers pp_answers)
  )

(defsketch example                  ;;Define a new sketch named example
  :title "Oh so many grey points"  ;;Set the title of the sketch
  :setup setup                      ;;Specify the setup fn
  :draw draw                        ;;Specify the draw fn
  :size [323 600])                  ;;You struggle to beat the golden ratio











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