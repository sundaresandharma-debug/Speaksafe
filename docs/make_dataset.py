# Generates backend/src/main/resources/dataset/speaksafe_dataset.csv
# Usage (from the project root):  python3 docs/make_dataset.py backend/src/main/resources/dataset/speaksafe_dataset.csv
import random, csv, sys
random.seed(2026)
when = ["tomorrow","after school","tonight","after class","this weekend","when you least expect it","next time I see you","on monday","after the match","soon"]
where = ["at school","at the park","at practice","in class","at the bus stop","outside the gym","in the hallway"]
adj = ["stupid","dumb","ugly","pathetic","worthless","useless","disgusting","brainless","clueless","annoying","lame","fat","gross","hopeless"]
noun = ["idiot","loser","moron","clown","freak","failure","joke","weirdo","nobody","waste of space","dumbass","creep","fool"]
hurt = ["hurt","beat up","punch","kick","kill","attack","smash","break","stab","jump"]
groups = ["immigrants","refugees","muslims","christians","jews","hindus","black people","asians","women","gay people","trans people","foreigners","disabled people","migrants","arabs","mexicans"]
neg = ["criminals","animals","parasites","inferior","dirty","vermin","a disease","subhuman","savages","trash","a plague","filthy"]
excl = ["be kicked out","go back where they came from","be banned","be removed","be thrown out","disappear"]
right = ["vote","work here","go to school","live here","have rights","be in our neighbourhood"]

insult_t = [
 lambda: f"you are so {random.choice(adj)}",
 lambda: f"you {random.choice(adj)} {random.choice(noun)}",
 lambda: f"nobody likes you, {random.choice(noun)}",
 lambda: f"shut up you {random.choice(adj)} {random.choice(noun)}",
 lambda: f"you're the most {random.choice(adj)} person I know",
 lambda: f"what a {random.choice(adj)} {random.choice(noun)} you are",
 lambda: f"go cry somewhere, {random.choice(noun)}",
 lambda: f"your face is so {random.choice(['ugly','gross','disgusting'])} lol",
 lambda: f"you can't do anything right, {random.choice(noun)}",
 lambda: f"everyone laughs at you, you {random.choice(noun)}",
 lambda: f"lol look at this {random.choice(adj)} {random.choice(noun)}",
 lambda: f"get lost {random.choice(noun)}, no one wants you here",
 lambda: f"you're such a {random.choice(noun)}",
 lambda: f"stop posting, you {random.choice(adj)} {random.choice(noun)}",
 lambda: f"imagine being this {random.choice(adj)}",
]
# directed profanity (swear words aimed at a person) is labelled as an insult
pnoun = ["bitch","bastard","asshole","dickhead","prick","motherfucker","piece of shit","dumb fuck","jackass","twat","scumbag","shithead"]
padj = ["fucking","stupid","worthless","pathetic","dumb","ugly","useless"]
insult_t += [
 lambda: f"fuck off {random.choice(['','you','loser','idiot','already','and never come back','nobody wants you'])}".strip(),
 lambda: f"fuck you {random.choice(['','and your','you','loser','bro','seriously','and everyone like you'])}".strip(),
 lambda: f"you {random.choice(padj)} {random.choice(pnoun)}",
 lambda: f"you are such a {random.choice(pnoun)}",
 lambda: f"what a {random.choice(pnoun)}",
 lambda: f"shut the fuck up {random.choice(['','you','loser','nobody asked'])}".strip(),
 lambda: f"go fuck yourself {random.choice(['','loser','idiot','you clown'])}".strip(),
 lambda: f"nobody gives a {random.choice(['shit','fuck','damn'])} about you",
 lambda: f"get the fuck out {random.choice(['','of here','of this group','loser'])}".strip(),
 lambda: f"{random.choice(['stfu','gtfo'])} {random.choice(['','you','loser','idiot','bitch'])}".strip(),
 lambda: f"your {random.choice(['opinion','face','music','work','post'])} is {random.choice(['shit','fucking trash','bullshit','crap'])}",
 lambda: f"eat shit {random.choice(['','loser','and die'])}".strip(),
 lambda: f"what the fuck is wrong with you",
 lambda: f"you {random.choice(['suck','fucking suck','are full of shit'])}",
]

threat_t = [
 lambda: f"I will {random.choice(hurt)} you {random.choice(when)}",
 lambda: f"I'm going to {random.choice(hurt)} you {random.choice(when)}",
 lambda: f"watch your back {random.choice(when)}",
 lambda: f"you better watch out or I'll {random.choice(hurt)} you",
 lambda: f"I know where you live, see you {random.choice(when)}",
 lambda: f"show up {random.choice(where)} and I will {random.choice(hurt)} you",
 lambda: f"you won't make it home {random.choice(when)}",
 lambda: f"say that again and I'll {random.choice(hurt)} you",
 lambda: f"I'll find you {random.choice(when)} and you'll regret it",
 lambda: f"we are coming for you {random.choice(when)}",
 lambda: f"you're dead {random.choice(when)}",
 lambda: f"wait till I catch you {random.choice(where)}",
 lambda: f"one more word and I will {random.choice(hurt)} you",
 lambda: f"I swear I'll {random.choice(hurt)} you if you tell anyone",
]
hate_t = [
 lambda: f"all {random.choice(groups)} are {random.choice(neg)}",
 lambda: f"{random.choice(groups)} are {random.choice(neg)} and should {random.choice(excl)}",
 lambda: f"{random.choice(groups)} don't belong in this country",
 lambda: f"I hate all {random.choice(groups)}",
 lambda: f"{random.choice(groups)} are ruining our country",
 lambda: f"we should get rid of all {random.choice(groups)}",
 lambda: f"{random.choice(groups)} are less than human",
 lambda: f"{random.choice(groups)} should not be allowed to {random.choice(right)}",
 lambda: f"never trust {random.choice(groups)}, they are all {random.choice(neg)}",
 lambda: f"{random.choice(groups)} should {random.choice(excl)}",
 lambda: f"our society would be better without {random.choice(groups)}",
 lambda: f"typical {random.choice(groups)}, always {random.choice(['stealing','lying','causing trouble','begging'])}",
]
pos = ["amazing","so kind","really smart","hilarious","a great friend","so talented","awesome","brilliant","helpful","the best"]
things = ["the notes","the project","your presentation","the homework","the photos","the recipe","the link","the video","your help","the birthday party"]
acts = ["playing football","reading novels","coding in java","cooking","painting","watching movies","going on hikes","playing chess","learning guitar","travelling"]
events = ["exam","meeting","match","concert","class trip","hackathon","seminar","assignment deadline"]
gpos = ["my {g} neighbours brought us food for the festival","the {g} community centre is hosting a free workshop","we had a great talk about the history of {g}",
        "proud of the {g} students who won the science fair","the documentary about {g} was really moving","many {g} work in our local hospital",
        "{g} are welcome to join our volunteer group","an interesting article on challenges faced by {g}"]
safe_t = [
 lambda: f"you are {random.choice(pos)}",
 lambda: f"thanks for {random.choice(things)}",
 lambda: f"see you {random.choice(when)}",
 lambda: f"great job on {random.choice(things)}",
 lambda: f"I love {random.choice(acts)}",
 lambda: f"can you send me {random.choice(things)}",
 lambda: f"the {random.choice(events)} is {random.choice(when)}",
 lambda: random.choice(gpos).format(g=random.choice(groups)),
 lambda: f"meet me {random.choice(where)} {random.choice(when)}",
 lambda: f"I will help you with {random.choice(things)} {random.choice(when)}",
 lambda: f"good luck with the {random.choice(events)}!",
 lambda: f"anyone up for {random.choice(acts)} {random.choice(when)}?",
 lambda: f"did you finish {random.choice(things)}?",
 lambda: f"haha that was {random.choice(['funny','awesome','wild','so good'])}",
 lambda: f"happy birthday! you are {random.choice(pos)}",
 lambda: f"don't forget {random.choice(things)} {random.choice(when)}",
 lambda: f"I'm going to {random.choice(['study','sleep','eat','practice','relax','go home'])} {random.choice(when)}",
]

names = ["Riya","Arjun","Meena","Kavin","Priya","Rahul","Sneha","Vikram","Anu","Rohit","Divya","Sam"]
places = ["the library","the canteen","the lab","the hostel","the auditorium","the bus stand","the ground","class","the office","the station"]
verbs = ["call","text","message","meet","help","pick up","wait for","join","email","remind"]
nice = ["nice","great","amazing","beautiful","happy","confident","smart","cool","so good","really talented"]
gneutral = ["the {g} students association meets every friday","we learned about festivals celebrated by {g}","{g} have made big contributions to science",
            "the seminar on rights of {g} was very informative","I read a book written by {g} authors","our team has {g} and everyone gets along",
            "respect for {g} should be taught in schools","the library has a new section on the history of {g}","{g} deserve equal opportunities",
            "volunteers helped {g} affected by the floods","there is a scholarship for {g} this year"]
regions = ["Kerala","Punjab","Chennai","Mumbai","Japan","Brazil","Bengal","Hyderabad","Assam","Italy","Nigeria","Kashmir"]
safe_t += [
 lambda: f"{random.choice(['sorry','my bad','I apologise','really sorry'])}, {random.choice(['I was upset','I was angry','I overreacted','that came out wrong','I was stressed'])}. {random.choice(['can we talk','let us talk','can we meet','call me'])} {random.choice(when)}?",
 lambda: f"{random.choice(['can we talk','let us talk','can we sort this out','can we meet'])} {random.choice(when)}?",
 lambda: f"{random.choice(['this exam','the traffic','this heat','my back','this assignment','the wait','maths','this project','the syllabus','my legs'])} is killing me {random.choice(['lol','haha','','bro','ugh'])}".strip(),
 lambda: f"you {random.choice(['killed it','nailed it','smashed it','crushed it'])} {random.choice(['on stage','in the quiz','today','in the match','bro'])}",
 lambda: f"I'm {random.choice(['dying of laughter','dead tired','dying to see it','so dead after practice'])} {random.choice(['lol','haha','😂',''])}".strip(),
 lambda: f"people from {random.choice(regions)} are so {random.choice(['friendly','welcoming','talented','hardworking','fun'])}",
 lambda: f"people from {random.choice(regions)} make {random.choice(['amazing food','beautiful music','the best tea','great films'])}",
 lambda: f"I want to visit {random.choice(regions)} {random.choice(when)}",
 lambda: f"I'll {random.choice(verbs)} you {random.choice(when)}",
 lambda: f"I will {random.choice(verbs)} you {random.choice(when)}",
 lambda: f"we'll {random.choice(['study','practice','meet','eat','revise'])} at {random.choice(places)} {random.choice(when)}",
 lambda: f"I'll {random.choice(['bring','share','upload','send','print','finish'])} {random.choice(things)} {random.choice(when)}",
 lambda: f"{random.choice(names)} is coming back {random.choice(when)}",
 lambda: f"she's going to {random.choice(places)} {random.choice(when)}",
 lambda: f"he's {random.choice(['busy','free','travelling','at home','in the lab'])} {random.choice(when)}",
 lambda: f"you look {random.choice(nice)} today",
 lambda: f"you're {random.choice(['welcome','right','the best','so kind','a legend','doing great'])}",
 lambda: f"you did {random.choice(['well','great','amazing'])} {random.choice(['in the exam','in the match','on stage','in the quiz'])}",
 lambda: random.choice(gneutral).format(g=random.choice(groups)),
 lambda: f"I can't {random.choice(['wait for','come to','find','believe'])} {random.choice(things)}",
 lambda: f"don't {random.choice(['forget','miss','be late for'])} the {random.choice(events)} {random.choice(when)}",
 lambda: f"is {random.choice(places)} open {random.choice(when)}?",
 lambda: f"{random.choice(names)}, are you free {random.choice(when)}?",
 lambda: f"the {random.choice(events)} got moved to {random.choice(when)}",
]

hand = {
"INSULT": ["fuck off","fuck you","f*ck off","f**k you","you piece of shit","what an asshole","stfu loser","fck off bitch","sh*t head","you stupid b*tch","a$$hole","f u","fu","honestly you're the dumbest kid in our class","nobody asked for your opinion, clown","you look like a potato lmao","ur so stupid its embarrassing",
 "go back to your cave, nobody wants you here","you're a total waste of oxygen","lol you failed again, what a loser","even your mom thinks you're annoying",
 "delete your account, you're pathetic","you write like a five year old idiot","why are you so ugly bro","your voice is so annoying, just stop talking",
 "you are an embarrassment to everyone","such a fake friend, two faced liar","you're worthless and everyone knows it","no wonder you have no friends, freak"],
"THREAT": ["go kill yourself loser","kys nobody wants you","just go die already","you should kill yourself","nobody would miss you if you died","go jump off a bridge","I'm going to make your life hell","keep talking and see what happens to you","you'll be sorry when I get my hands on you",
 "better not come to school tomorrow if you know what's good for you","I'll post your address so everyone can come after you","next time it won't just be words",
 "I'm gonna break your face","sleep with one eye open","you have no idea what's coming for you","I will burn your house down",
 "send me money or I'll share your photos","we'll be waiting for you outside","I'll make you disappear","touch my stuff again and you're dead"],
"HATE_SPEECH": ["people of that religion are all terrorists","that whole race is born stupid","women belong in the kitchen not in parliament",
 "send them all back on boats","they breed like rats and take our jobs","no place for their kind in our town","gay people are sick and need to be fixed",
 "those people are a cancer on society","disabled people are a burden and should not be born","their culture is barbaric and inferior",
 "keep those foreigners out of our schools","they are invaders, not humans"],
"SAFE": ["the bus was late again today","what time does the library close?","this game is killing me haha, so hard","I'm dead tired after practice lol",
 "can't believe how good that movie was","mom made biryani tonight","who's bringing snacks for the trip?","congrats on getting into the team!",
 "the teacher extended the deadline to friday","my phone battery is dying","that exam destroyed me but I passed","let's form a group for the project",
 "the new cafeteria menu is actually good","please share the zoom link","I disagree with your point but I respect it","that was a sick goal in the last minute",
 "my brother beat me at chess again","the wifi in the hostel is terrible today","I hate mondays honestly","this weather is so gloomy",
 "you killed it on stage yesterday!","nice try, better luck next time","stop worrying, you'll do great","is anyone else stuck on question 4?",
 "the referee was so bad in that match","I'm feeling a bit sad today","thanks everyone for the birthday wishes","lol my code finally compiled",
 "ok","see you","good night everyone","welcome to the group!",
 "thank you so much for the notes","pls share the ppt in the group","which room is the viva in?","congrats on the internship!!",
 "bring your id card tomorrow","does anyone have a spare charger","the placement talk starts at 10","get well soon bro",
 "good luck everyone for the semester","the canteen dosa is the best","shall we order pizza for the team","lab is cancelled today yay",
 "you're a lifesaver, thanks","proud of you","love this photo of the trip","sending you the form now","ok sure, see you there",
 "my laptop crashed again","that was a close match","the sunset from the terrace was beautiful","the new syllabus looks tough",
 "who is taking attendance today","we should start a coding club","I'll be a little late, start without me","missed the bus again",
 "happy diwali everyone","eid mubarak to all","merry christmas friends","happy pongal!","hope your exam went well",
 "damn, this traffic is terrible","what the heck happened in class","oh crap I forgot my id card","hell yeah we won the match","damn that was a good movie","this exam is so hard, damn","crap, the bus left already","what the hell, the wifi is down again",
 "you too","thank you","and you?","miss you guys","you there?","love you all","you first","after you","you as well","bless you","you okay?","same to you",
 "can we reschedule the meeting","just reached home","let's revise together tonight","the results are out!","hey, how are you?",],
}

import os
EXTRA_SAFE = os.path.join(os.path.dirname(os.path.abspath(__file__)), "dataset_extra_safe.txt")
hand["SAFE"] += [l.strip() for l in open(EXTRA_SAFE, encoding="utf-8") if l.strip() and not l.startswith("#")]

def gen(templates, n, label):
    out = set(t for t in hand[label])
    if label == "SAFE":
        # every group gets the same number of positive / neutral sentences,
        # so the model learns that naming a group is not hate speech by itself
        for g in groups:
            for t in random.sample(gpos + gneutral, 5):
                out.add(t.format(g=g))
    tries = 0
    while len(out) < n and tries < 100000:
        out.add(random.choice(templates)()); tries += 1
    return [(label.lower(), t) for t in sorted(out)]   # sorted -> same file every run

rows = gen(safe_t,980,"SAFE")+gen(insult_t,260,"INSULT")+gen(threat_t,140,"THREAT")+gen(hate_t,140,"HATE_SPEECH")
random.shuffle(rows)
w = csv.writer(open(sys.argv[1],"w",newline=""), quoting=csv.QUOTE_MINIMAL, lineterminator="\n")
w.writerow(["label","text"])
for r in rows: w.writerow(r)
print(len(rows))
