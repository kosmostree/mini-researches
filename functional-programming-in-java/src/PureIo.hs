-- getLine  :: IO String
-- putStrLn :: String -> IO () 

greet :: IO ()
greet = do
  putStrLn "이름은?"
  name <- getLine 
  putStrLn ("내 이름은: " ++ name)