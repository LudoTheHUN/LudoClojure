(ns LudoClojure.javainterop)

(import '(java.awt AWTException Robot Rectangle Toolkit Component)
        '(java.io File IOException PushbackReader FileReader)
        '(java.awt.Robot.)
        '(java.awt.image BufferedImage DirectColorModel PixelGrabber)
        '(javax.imageio ImageIO)
        '(java.awt Color Graphics Graphics2D Dimension Point BorderLayout FlowLayout Window Frame GraphicsConfiguration )
        '(java.awt.event MouseMotionListener HierarchyBoundsListener)
           
        '(java.awt Graphics)
        '(javax.swing.event ChangeListener ChangeEvent)  
        '(javax.swing JPanel JFrame JButton JWindow)
        )

;;great docs on interop functionality...
;;http://kotka.de/blog/2010/02/gen-class_how_it_works_and_how_to_use_it.html
;;http://kotka.de/blog/2010/03/proxy_gen-class_little_brother.html


(defn GrabScreen [x y xd yd]
  "creates buffeed image object"
       (.createScreenCapture (Robot.)
                 (Rectangle. x y xd yd)))

(time (GrabScreen 10 10 100 100))
(time (.getColorModel (GrabScreen 10 10 200 200)))
(.getData (GrabScreen 10 10 200 200))
(.getHeight (GrabScreen 10 10 200 200))


(def screengrab (GrabScreen 10 10 200 200))
(time (let [colour (Color. (.getRGB screengrab 2 2 ))]
   [(.getRed colour)
    (.getGreen colour)
    (.getBlue colour)
    ]))



(.getRGB (.getSubimage (GrabScreen 10 10 200 200) 5 5 5 5) 1 2)
(.toString (.getSubimage (GrabScreen 10 10 200 200) 5 5 5 5))

(defn bg_change! [a_bg]
   (doto a_bg  ;;this will change the state of bg, which is a child of img, thus changing img, java is so wrong!
      (.setColor (Color. 13123))
      (.fillRect 0 0 20 40)))
  

(defn render [g]
    (let [img ;;(new BufferedImage (* scale dim) (* scale dim) 
              ;;   (. BufferedImage TYPE_INT_ARGB))
               (GrabScreen 10 10 200 200)
           bg (. img (getGraphics))
           ]
    ;(doto bg  ;;this will change the state of bg, which is a child of img, thus changing img, java is so wrong!
    ;  (.setColor (Color. 123))
    ;  (.fillRect 0 0 20 20))
    (bg_change! bg)
    (. g (drawImage img 0 0 nil))
    ;;(. bg (dispose)))
    ))


(def panel (doto (proxy [JPanel] []
                        (paint [g] (render g)))   ;;'paint'  is a method inhereted from JComponent
                 (.setPreferredSize (new Dimension 150 250))))    ; '.setPreferredSize'  is also a method inhereted from JComponent

(def panel2 (doto (JPanel.)
                  (.paint (. (GrabScreen 10 10 200 200) (getGraphics)))
   ;;'paint'  is a method inhereted from JComponent
                 (.setPreferredSize (new Dimension 150 250))))

;repaints the pannle, this means the 
(.repaint panel)

(defn frame []     ;;This is called from the core as entry point the UI and program
        (doto 
             (proxy [JFrame] ["fun frame"])
             (.add panel)
             .pack
             .show))

(defn frame2 []     ;;This is called from the core as entry point the UI and program
        (doto 
             (proxy [JFrame] ["fun frame two"])
             ;(.add panel)
             .pack
             .show))

(defn frame3 []     ;;This is called from the core as entry point the UI and program
        (doto 
             (proxy [Frame] ["fun frame three"])
             ;(.add panel)
             .pack
             .show))

;(def theframe3(frame3))




(defn component1 []     ;;This is called from the core as entry point the UI and program
        (doto 
             (proxy [Component] [])
             ;(.add panel)
            ; .pack
             (.setVisible true)
  ))


;(component1)


(defn EyeEdge [x y]     ;;This is called from the core as entry point the UI and program
        (doto 
             (proxy [JFrame] [])
             (.setLayout nil)
             (.setResizable false)
             (.setAlwaysOnTop true)
             (.setBounds x y 20 20) 
             ;(.setUndecorated true)
             .pack
             (.setVisible true)
  ))


(defn EyeEdges [] (do
   {:a (EyeEdge 400 400)
    :b (EyeEdge 480 490)
} ))

(def theEyeEdges (EyeEdges))

(println (:a theEyeEdges ))


(.getLocation (theEyeEdges :a))
(.getLocation (theEyeEdges :b))


(defn EyeView [x y]     ;;This is called from the core as entry point the UI and program
        (doto 
             (proxy [JFrame] [])
             
             (.setResizable false)
             (.setAlwaysOnTop true)
             (.setBounds x y 200 200) 
             ;(.setUndecorated true)
             (.setVisible true)
  ))

(def theEyeView (EyeView 600 600))
(.getBounds theEyeView)


(doto (theEyeEdges :b)  ;to this to bottom right endge of scan area
                   (.addMouseMotionListener  (proxy [MouseMotionListener] []    
                                         (mouseMoved [mouse_evt]
                                           (let [val mouse_evt]
                                            (do 
                                              (println "mousemovedhere with " mouse_evt)
                                              ;(swap! kernelrandmoderoutput_clj (fn [_] val))
                                                ;(runCL)
                                                ;(.repaint panel)                         ;do 2nd thing
                                                ;(println "vertical slider got set to :" val )) ;do 3rd thing
                                                ))))))

(doto (.getContentPane theEyeView)
    (.addHierarchyBoundsListener (proxy [HierarchyBoundsListener] [] 
                           (ancestorMoved [evt]
                                   (do 
                                     (println "ansestor got moved" evt)))
                           (ancestorResized [evt]
                                   (do 
                                     (println "ansestor got resized" evt)))
                           ))
                           )





(quote

)


(quote
(doto (theEyeEdges :a) (.setVisible false) (.setUndecorated false) (.setVisible true))
(.getBounds
(.getGraphicsConfiguration (theEyeEdges :a)) 
  )

(doto (proxy [Robot] [])
(.mouseMove 440 450))



(defn theJWindow1 []     ;;This is called from the core as entry point the UI and program
        (doto 
             (proxy [JWindow] [])
             ;(.add panel)
            ; .pack
             
             (.setSize (Dimension. 30 30))
             (.setVisible true)
  ))

(defn theJWindow2 []     ;;This is called from the core as entry point the UI and program
             (proxy [JWindow] []))

(def jwin2 (theJWindow2))
(. jwin2 setAlwaysOnTop true)
(. jwin2 setSize (Dimension. 30 40))
(. jwin2 setVisible true)
;(setLocation. jwin2 1000 752)

(doto 
   jwin
   (.setLocation 900 652))
;;OR
(. jwin setLocation 1000 752)   ;; (. object1 method2 arguments3)
(. jwin setLocation 1000 800)
(. jwin setAlwaysOnTop true)
;;OR
;;this should work?! (setLocation. jwin 1000 752)   ;;(method2. object1 arguments3)
   

(.repaint panel)

(def theFrame (frame))
(def theFrame2 (frame2))
(doto 
   theframe3
   ;(.getRootPane theFrame2)
   ;(.setLocationRelativeTo (.getRootPane theFrame2))
   ;(.setLayout (BorderLayout.))
   ;(.setLayout (FlowLayout.))
   (.setLayout nil)
   ;;(.setPreferredSize 15 30)
   ;(.setLocation 769 452)
   ;;;(.setLocationOnScreen 100 100)
   (.setResizable false)   ;;need this to get consistant behaviour
   (.setBounds 400 401 5 5) 
   ;  (.setOpaque true)
   (.setAlwaysOnTop true)
   .pack
 ;  (.setSize (Dimension. 30 30)) 
    ;(.setSize 35 35) 
   (.setVisible true)
 ;  (.setOpaque true)
   ;.show
  )
(doto 
   (.getRootPane theFrame2)
   (.setLayout (FlowLayout.)))
   
 
(new Point 10 10)
(Point. 10 10)
(Dimension. 10 10)
(BorderLayout.)

(.getLocation theFrame2)
(.getLocationOnScreen theFrame2)


(.getSize theframe3)
(.getX theFrame2)
(.getY theFrame2)
(.getParent theFrame2)
(.getLocation (.getRootPane theFrame2))
(.getLocation (.getRootPane (.getRootPane theFrame2)))
(.getLocation (.getRootPane (.getRootPane (.getRootPane theFrame2))))

(defn- to-dim-array [x]
  (cond
    (number? x) (int-array [x])
    (sequential? x) (into-array x)))


(to-dim-array 12)
(to-dim-array [12 23])


;;enqueue-kernel   see it we can design 2D or 3D kernels...

)



