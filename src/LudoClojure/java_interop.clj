(ns LudoClojure.java_interop)

(import '(java.awt AWTException Robot Rectangle Toolkit)
        '(java.io File IOException PushbackReader FileReader)
        '(java.awt.Robot.)
        '(java.awt.image BufferedImage DirectColorModel PixelGrabber)
        '(javax.imageio ImageIO)
        '(java.awt Color Graphics Graphics2D Dimension)
        
        '(java.awt Graphics)
        '(javax.swing.event ChangeListener ChangeEvent)
        '(javax.swing JPanel JFrame JButton)
        )


(defn GrabScreen [x y xd yd]
  "creates buffeed image object"
       (.createScreenCapture (Robot.)
                 (Rectangle. x y xd yd)))


(defn render [g]
    (let [img ;;(new BufferedImage (* scale dim) (* scale dim) 
              ;;   (. BufferedImage TYPE_INT_ARGB))
               (GrabScreen 10 10 200 200)
           ;;bg (. img (getGraphics))
           ]
      
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

(.repaint panel)

(defn frame []     ;;This is called from the core as entry point the UI and program
        (doto 
             (proxy [JFrame] ["fun frame"])
             (.add panel)
             .pack
             .show))


(frame)



(defn- to-dim-array [x]
  (cond
    (number? x) (int-array [x])
    (sequential? x) (into-array x)))


(to-dim-array 12)
(to-dim-array [12 23])


;;enqueue-kernel   see it we can design 2D or 3D kernels...





