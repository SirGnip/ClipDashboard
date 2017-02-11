# ClipDashboard - What is it?

Do you like using command line tools to manipulate text and would like to have similar functionality on the text in your clipboard? Do you work with a lot of text and your clipboard is your constant companion?  ClipDashboard adds a some tools that interact directly with your clipboard and allow you to do some more advanced text processing on the text in your clipboard.

# Features

## In-place manipulations of the text currently stored in your clipboard:
- trim whitespace, convert case, prepend, append, split, replace
- treat clipboards containing multiple lines of text as a "list" and do list style operations on the lines in that list:
    - trim whitespace from each line
    - remove empty lines and duplicates
    - reverse order or sort
    - filter by substring or regex match
    - basic stats (character count, word count, line count, min/max/avg line length)

## Clipboard buffers
- Store different snippets of text from your clipboard in multiple buffers.
- Retrieve the contents of any buffer at any time
- Automatically cycle through selected buffers with each click of "Retrieve"
- Open a diffing tool to show difference between the contents of two buffers

# Setting Expectations

This is a JavaFX UI application I am writing while learning Java.  I don't like writing "toy" programs so I decided to do the ye ol' "scratch your own itch."  So, I wanted something easy to write, yet serve an actual purpose.
  
This is a work in progress and I make no promises as to how complete, beautiful, useful, elegant or robust it currently is.