# To Do
- x create project directory to work with
- x hello world in java via command line toolchain
- x build simple project in intellij
- x command line parsing
    - x positional args
    - x arbitrary number of positional args
    - x optional args
- x read Java tutorial and document in my cheat sheet and matrix
    - skim my cheat sheet to get context
- core language features
    - config file (properties file?)
- x hello world in intellij ( can i also build via cmd line?)
- x move these notes into java project space
- x minimal JavaFX app (copy snippet into cheat sheet.)
- screen-saverish demo-ish JavaFX app
    - play with tesselation, actors with random movement, alpha blending, initial kineticville tinkering
- JavaFX interacting with clipboard
- Java demoscene demo (demonstrating a few different effects with music, possibly a simple story/theme to tie it all together)

# Roadmap

- clipboard modification CLI tool (get, set, append, split)
    - use cli library. try to do something real for a bit
    - should only do this for a couple days
- command line joystick test
- JavaFX joystick demo test app
- java clipboard toolbox UI
    - x get JavaFX going
    - x when app opens, read clipboard and put it in a buffer (have some visual on the UI to show that it queried the clipboard)
    - able to split, join, prepend, append, replace, sort, filter, columns, graph, clipboard stack, etc.
    - variable substitution?
    - push and retrieve from other buffers
    - able to treat buffers and deque (push/pop from front and back) and random access (read from anywehre)
    - clipboard stack/cache/manipulator
    - manage a clipboard stack
    - allow modifications (regex, substitution, replacement, stripping, append/prepend, numbering) to clipboard in memory and 
clipboard buffers
    - different ways of getting text in (take a multi-line selection and put each line into a separate bucket? separate by a comma?)
        - drag and drop replaces
        - drag and drop appends (maybe with optional delimiter)
    - different ways of getting text out (clipboard, save to file, POST to URL?)
        - post clipboard to url
        - use variable substitution to open a url (ex: http://www.stuff.com/doit/%{1}
    - variable substitution
        - be able to independently set values for aaa=somestring bbb=other and substitute those into a sequence of clips 
(if clip contains "this is ${aaa} for ${bbb}". 
    - open url aftre substituting current clipboard into a url (ex: http://docs.oracle.com/javase/8/docs/api/javax/swing/SpinnerDateModel.htm
l (any way to search for docs given just a class/interface name?)

