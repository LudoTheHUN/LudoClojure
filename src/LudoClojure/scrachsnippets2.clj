;Java interop examples
(import '(java.awt AWTException Robot Rectangle Toolkit)
        '(java.io File IOException)
        '(java.awt.Robot.)
        '(java.awt.image BufferedImage)
        '(javax.imageio ImageIO))


(. (. System (getProperties)) (get "os.name"))

(comment  show the Nano Time)
(. System (nanoTime))


;comment http://download.oracle.com/javase/1.4.2/docs/api/java/awt/Robot.htmls

(.mouseMove (java.awt.Robot.) 100 100)

gets the color oject for a pixel
(.getPixelColor (java.awt.Robot.) 100 100)

(.getPixelColor (java.awt.Robot.) 100 100)

(.getRed (.getPixelColor (java.awt.Robot.) 100 100))
(.getBlue (.getPixelColor (java.awt.Robot.) 100 100))
(.getGreen (.getPixelColor (java.awt.Robot.) 100 100))


;why fail?
;http://www.gettingclojure.com/cookbook:system
;This needs the inports as above
(.createScreenCapture (Robot.)
  (Rectangle. (.getScreenSize (Toolkit/getDefaultToolkit))))

(.createScreenCapture (Robot.)
  (Rectangle. 10 20 20 40))

(ImageIO/write (.createScreenCapture (Robot.)
                 (Rectangle. 10 20 20 40))
               "JPG"
               (File. "/home/ludo/Documents/cljoutfile.jpg"))





http://download.oracle.com/javase/1.4.2/docs/api/java/awt/Image.html

Buffered image, vs SunGraphics2D... one lets you manipulate the graphics others let you draw them...
FAIL (.drawImage (.createScreenCapture (Robot.)
  (Rectangle. 10 20 20 40)) 10 10 100 200)
  
(.createGraphics (.createScreenCapture (Robot.)
  (Rectangle. 10 20 20 40)))

....WIP
(.draw (.createGraphics (.createScreenCapture (Robot.)
  (Rectangle. 10 20 20 40))))
(draw (Rectangle. 10 20 20 40))
....

;local veriable
(let [a 10] (println a))


