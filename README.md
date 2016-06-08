multisnake
==========

**This is no longer developed with last commits in 2008.**

<img src="https://github.com/mikexstudios/multisnake/blob/master/screenshots/gameplay1.gif" alt="home">

a massively multiplayer snake flash game

Ever wondered what would happen if you took the traditional snake game and the
made it *massively* **multiplayer**? Look no further! Play with (potentially)
**hundreds of other people** on the internet.

This game was a bit ahead of its time given the rise of massively multiplayer
browser games that are popular today (e.g. [TagPro][tp] and [Agar.io][ag]).
At the time, websockets weren't around so we decided to write the client in 
Flash. [mikexstudios][mx] worked on the client and website while 
[eugeneiiim][em] developed the server and map editor. We put the game together
in about a few days over winter break. Eventually, this game became a hit at
Palantir and inspired their [blog post about making a clone of Multisnake on
their platform][palantir].

[tp]: http://tagpro.koalabeast.com/
[ag]: http://agar.io/
[mx]: https://github.com/mikexstudios
[em]: https://github.com/eugeneiiim
[palantir]: http://www.palantir.com/2009/07/the-multisnake-challenge/ 

## Screenshots

<table>
<tr>
  <td align="center">
   <img src="https://github.com/mikexstudios/multisnake/blob/master/screenshots/home.png" width="100%">
   <p><strong>Home</strong></p>
  </td>
  <td align="center">
    <img src="https://github.com/mikexstudios/multisnake/blob/master/screenshots/instructions.png" width="100%">
    <p><strong>Instructions</strong></p>
  </td>
</tr>
<tr>
  <td align="center">
   <img src="https://github.com/mikexstudios/multisnake/blob/master/screenshots/connecting.png" width="100%">
   <p><strong>Connecting</strong></p>
  </td>
  <td align="center">
    <img src="https://github.com/mikexstudios/multisnake/blob/master/screenshots/play1.png" width="100%">
    <p><strong>Gameplay 1</strong></p>
  </td>
</tr>
<tr>
  <td align="center">
   <img src="https://github.com/mikexstudios/multisnake/blob/master/screenshots/play2.png" width="100%">
   <p><strong>Gameplay 2</strong></p>
  </td>
  <td align="center">
    <img src="https://github.com/mikexstudios/multisnake/blob/master/screenshots/play3.png" width="100%">
    <p><strong>Gameplay 3</strong></p>
  </td>
</tr>
<tr>
  <td align="center">
   <img src="https://github.com/mikexstudios/multisnake/blob/master/screenshots/score.png" width="100%">
   <p><strong>Score Screen</strong></p>
  </td>
  <td align="center">
    <img src="https://github.com/mikexstudios/multisnake/blob/master/screenshots/shareshot.png" width="100%">
    <p><strong>Sharing</strong></p>
  </td>
</tr>
</table>

## Play Instructions

- **Goal**:	Your task is to obtain the highest score by eating food, defeating
  opponent snakes, and staying alive as long as possible. The fittest snake
  wins!

- **Controls**:	The arrow keys control the movement of the snake.

- **Scoring**: Each food piece you eat and each snake that you kill (defined as
  getting another snake to run into your body) gains you 5 points. Conversely,
  each time you die costs you 5 points!

- **Tip**: One-unit snakes cannot kill other snakes! (Conversely, this means
  that you can eat one-unit snakes as food.) Therefore, when you are a one-unit
  snake, make sure you eat food as soon as possible so that you aren't
  defenseless!

## Usage

1. Build the `Dockerfile` which will set up a local Google appengine, flash
   compiler, and Java SDK:

   `docker build -t mikexstudios/multisnake .`

2. Run it like:

   `docker run -d -p 80:80 -p 843:843 -p 10123:10123 mikexstudios/multisnake`

   If you want to develop while running the script, mount the current 
   directory by:

   ```docker run -d -p 80:80 -p 843:843 -p 10123:10123 `pwd`:/usr/src/app mikexstudios/multisnake```

3. Access the IP address of the docker container in a web browser to use!
