# Alpha.99 testing notes

- G displays the selected fruit's moves with or without the Holy Bag. Help wraps to the current GUI width and remains visible for eight seconds in a separate panel; form timers cannot replace it. Ordinary Creeperborn notifications also wrap.
- G previews another fruit without ending the current form. Pressing a move or attacking with the selected fruit transfers out of the old fruit's form and clears its active effects. Merely moving, mounting a boat or opening a chest does not trigger the transfer. Element combos remain independent.
- The dragon can lose two heads. Its final head stays attached until shared boss health reaches zero. The body is vulnerable throughout that final phase, and attacking the final head also damages boss health. Existing active headless encounters are repaired on load.
- Glint flight now explicitly updates the server's player chunk tracking after accepted custom movement, so chunk streaming follows Light Body.

Replace the JAR in both server and client mods folders with Minecraft closed and the server stopped. Test G at a large GUI scale, then use a new fruit while a prior form is carried. Test sustained Glint flight across chunks and the dragon's one-head final phase.
