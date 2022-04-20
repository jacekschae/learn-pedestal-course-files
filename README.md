![alt learn-pedestal-logo](https://res.cloudinary.com/schae/image/upload/f_auto,q_auto:good,r_12/v1647616235/learnpedestal.com/1200x640.png)

# [LearnPedestal.com](https://www.learnpedestal.com)

Video course about Pedestal - REST API for Clojure. Including Pedestal, Component, Transit , Datomic Cloud, Datomic dev-local, Datomic Ions, and AWS deployment.

## Course files

The code in this repo includes two folders - `increments` - code for the start of each video (if you get lost somewhere along the way just copy the content of the video you are starting and continue). `cheffy` this is the start of the project / course. It's the same code as in `increments/06-start`

### Clone

```shell
$ git clone git@github.com:jacekschae/learn-pedestal-course-files.git

$ cd learn-pedestal-course-files/increments/<step-you-want-to-check-out>
```

### Run REPL

Probably you will run your REPL from your editor, and thre is nothing stopping you to run it from the command line:

```shell
clj
```

### Run the app
Probably you will run your REPL from your editor, and thre is nothing stopping you to run it from the command line:

```shell
clj -M:dev src/main/cheffy/server.clj
```

## License

Copyright © 2022 Jacek Schae