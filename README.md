<h1>TheLighterBot, version 3</h1>
Hi! Welcome to this project. It used to be privately developed but for some reason I thought it was time to publish what I'm doing to a more general public.

The previous version of this bot will never see the light of day, it's sort of a hackjob and getting around maintaining it has become sort of a hassle; hence my decision to start the entire thing from scratch.

<h3>History</h3>
When it first started out, I had pretty ambitious plans for the bot. It had a pretty weird math interpreter in it combined with a dynamic command loader. In essence, it encompassed a JSON file with the specifications of all commands offered by the bot. Once a command was issued, a new class would be created, exported and instantiated with the contents of a part of the JSON file. In that way I could dynamically add new commands and just reload once I had an update.

Then I entered a server which had an ever growing list of channels and had the idea of bringing back the temporary channels, TeamSpeak style. This is where version 2 emerged and slowly expanded its features over time. This meant it got features that were never really planned and hence lacked more and more structure.

As of now, it features commands for creating channels, making them permanent, linking channels together (displaying when people join/leave), having a blacklist (so certain people/roles can't interact with the bot), etc. Oh, and it does time conversions!

<h3>Plans</h3>
For now, this will purely be a rebuild. This means its sole purpose will be to build the features back up with a proper structure backing them. Possibly things will be added if the time comes, but it would have to be pretty major.

<h3>Technology</h3>
For this, I'm using IntelliJ IDEA Ultimate as IDE. Added to that I'm using the JDA library for interfacing with Discord and Gson for interacting with the JSON config files.
