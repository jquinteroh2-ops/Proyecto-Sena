"""GA8_AA1_EV01_IIN - Emerging Technologies (documento en ingles, maximo 6 paginas)."""
import json
import re

from comun import REPO, documento, imprimir, uri

MOD = REPO / "poc-ml" / "modelo"
m = json.loads((MOD / "metricas.json").read_text(encoding="utf-8"))["modelos"]["Regresion logistica"]
ADVERBIOS = ["already", "lately", "still", "yet", "ever", "just"]


def adv(texto):
    """Highlights the target adverbs so the instructor can find them quickly."""
    return re.sub(r"\b(" + "|".join(ADVERBIOS) + r")\b",
                  r'<mark style="background:#FEF3C7;color:#92400E;border-radius:4px;padding:0 3px;font-weight:700">\1</mark>',
                  texto, flags=re.IGNORECASE)


CAMPOS = [
    ("🏥", "Healthcare", "Neural networks already detect diseases in X-rays."),
    ("🏦", "Banking", "Algorithms spot fraud in real time."),
    ("🛒", "E-commerce", "Recommendation systems suggest products."),
    ("🚗", "Transport", "Deep learning powers self-driving cars."),
    ("🔐", "Cybersecurity", "Pattern recognition finds attacks."),
    ("🎓", "Education", "Predictions flag students at risk early."),
]

PASOS = ["Training data", "Algorithm learns patterns", "Machine learning model", "Predictions on new data"]

campos_html = "".join(
    f'<div style="border:1px solid #DDE2EC;border-radius:12px;padding:8px 10px;background:#fff">'
    f'<div style="font-size:20px">{ico}</div><div style="font-weight:700;color:#2350D6">{t}</div>'
    f'<div style="font-size:9pt;color:#4B5563">{adv(d)}</div></div>' for ico, t, d in CAMPOS)

pasos_html = "".join(
    f'<div style="flex:1;background:{c};color:#fff;border-radius:10px;padding:10px 8px;text-align:center;font-weight:600;font-size:9.5pt">{p}</div>'
    + ('<div style="align-self:center;font-size:18px;color:#6B7280;padding:0 4px">➜</div>' if i < 3 else "")
    for i, (p, c) in enumerate(zip(PASOS, ["#2350D6", "#4A3FD8", "#5B35D9", "#6D2BD9"])))

cuerpo = f"""
<section style="height:240mm;display:flex;flex-direction:column;justify-content:space-between;break-after:page">
  <div style="background:linear-gradient(135deg,#2350D6,#6D2BD9);color:#fff;border-radius:18px;padding:40px 34px">
    <div style="font-size:10pt;letter-spacing:.1em;text-transform:uppercase;opacity:.9">SENA · English B1.2+ · GA8_AA1_EV01_IIN</div>
    <h1 style="font-family:Georgia,serif;font-size:34pt;margin:22px 0 6px">Emerging Technologies</h1>
    <div style="font-size:15pt;opacity:.95">Machine Learning for an early-warning system in EduckTrack</div>
    <div style="margin-top:30px;display:flex;gap:8px;flex-wrap:wrap">
      {''.join(f'<span style="background:rgba(255,255,255,.18);border-radius:99px;padding:3px 12px;font-size:9.5pt">{t}</span>' for t in ['Machine Learning', 'Blockchain', 'Generative AI', 'Internet of Things', 'Recommendation systems'])}
    </div>
  </div>
  <div style="border:1px solid #DDE2EC;border-radius:14px;padding:18px 22px;font-size:10.5pt">
    <b>Students:</b> José Antonio Quintero Herrera · María José Grijalba Torres<br>
    <b>Program:</b> Software Analysis and Development (228118) · <b>Group:</b> <span class="pendiente">[ficha number]</span><br>
    <b>Instructor:</b> <span class="pendiente">[English instructor]</span> · <b>Date:</b> October 2026<br>
    <span style="color:#6B7280;font-size:9pt">The six target adverbs are <mark style="background:#FEF3C7;color:#92400E;padding:0 3px">highlighted</mark> in the text.</span>
  </div>
</section>

<h2 style="font-family:Georgia,serif">Section 1 · Exploring emerging technologies</h2>
<p>{adv("Have you ever wondered how your phone knows which video you want to watch next? That is <b>machine learning</b>: a branch of <b>artificial intelligence</b> in which <b>algorithms</b> learn <b>patterns</b> from <b>data sets</b> instead of following rules written by a programmer.")}</p>
<div style="display:flex;gap:4px;margin:8px 0 12px">{pasos_html}</div>
<p>{adv("In <b>supervised learning</b> the model studies labelled examples (students who passed or failed); in <b>unsupervised learning</b> it groups data without labels. <b>Deep learning</b> uses <b>neural networks</b> with many layers. Machine learning has already transformed many industries, and lately it has become crucial in cybersecurity.")}</p>
<h3>Where is machine learning used?</h3>
<div style="display:grid;grid-template-columns:repeat(3,1fr);gap:8px;margin-bottom:10px">{campos_html}</div>
<table class="t"><tr><th style="width:20%">Technology</th><th>What it does</th><th style="width:30%">Risk to consider</th></tr>
<tr><td><b>Blockchain</b></td><td>{adv("A shared ledger of blocks linked by hashes, so records cannot be changed. Cryptocurrencies such as Bitcoin still depend on it.")}</td><td>Data cannot be corrected or deleted.</td></tr>
<tr><td><b>Generative AI</b></td><td>Creates text or images from a prompt; useful for chatbots and tutors.</td><td>It can invent wrong answers.</td></tr>
<tr><td><b>Internet of Things</b></td><td>Sensors and readers that send data automatically, for example attendance with QR or NFC cards.</td><td>Privacy and hardware costs.</td></tr>
<tr><td><b>Recommendation systems</b></td><td>Suggest learning materials based on each student's grades.</td><td>Students may be labelled.</td></tr>
</table>

<h2 style="font-family:Georgia,serif">Section 2 · Machine Learning vs. Blockchain</h2>
<table class="t"><tr><th style="width:22%">Criterion</th><th>Machine Learning</th><th>Blockchain</th></tr>
<tr><td><b>Purpose</b></td><td>Learns from data to make <b>predictions</b>.</td><td>Keeps records that cannot be changed.</td></tr>
<tr><td><b>Data</b></td><td>Historical <b>training data</b>: grades, attendance.</td><td>Transactions, certificates.</td></tr>
<tr><td><b>Resources</b></td><td>Python, scikit-learn, a normal laptop.</td><td>Network nodes, keys, fees.</td></tr>
<tr><td><b>Complexity</b></td><td>Medium.</td><td>High.</td></tr>
<tr><td><b>Fit for EduckTrack</b></td><td><b>High</b>: the data already exists.</td><td>{adv("Low: grades must still be correctable.")}</td></tr>
</table>

<h2 style="font-family:Georgia,serif">Section 3 · Our choice: Machine Learning</h2>
<div class="caja">{adv("<b>The problem:</b> teachers often find out that a student has failed only when the term has already finished. EduckTrack has not yet been able to warn them earlier.")}</div>
<ul>
<li>{adv("<b>Who benefits:</b> teachers and academic coordinators, who can help students in time.")}</li>
<li>{adv("<b>Why it fits:</b> EduckTrack already stores grades and attendance and shares them through its REST API.")}</li>
<li>{adv("<b>Why not Blockchain:</b> grades need corrections, and an immutable ledger does not allow them.")}</li>
<li>{adv("<b>Ethics:</b> the model only warns the teacher; it never makes decisions about a student, and it does not use sensitive data such as gender or age.")}</li>
</ul>

<h2 style="font-family:Georgia,serif">Section 4 · Proof of concept design</h2>
<p>{adv("We have just finished training a <b>machine learning model</b> called <i>Early Academic Risk Alert</i>. It reads five signals in the middle of the term: partial average, number of grades below 3.0, trend, attendance and the previous average.")}</p>
<div class="grid2">
<figure><img src="{uri(MOD.parent.parent / 'docs' / 'evidencias-ga8' / 'graficas-en' / 'comparison.png')}"><figcaption>{adv("We compared three algorithms; logistic regression was the best one.")}</figcaption></figure>
<figure><img src="{uri(MOD.parent.parent / 'docs' / 'evidencias-ga8' / 'graficas-en' / 'confusion_matrix.png')}"><figcaption>It found 68 of the 78 students who failed.</figcaption></figure>
</div>
<table class="t">
<tr><td style="width:24%"><b>Input</b></td><td>Grades and attendance from the EduckTrack API (JWT login).</td></tr>
<tr><td><b>Processing</b></td><td>Logistic regression trained on 1,500 synthetic students (no real personal data).</td></tr>
<tr><td><b>Output</b></td><td>Risk probability, level (HIGH, MEDIUM, LOW) and the main reason.</td></tr>
<tr><td><b>Results</b></td><td>Recall {m['recall']:.0%} · AUC {m['auc_roc']} · 8 of 8 acceptance criteria passed.</td></tr>
<tr><td><b>Next step</b></td><td>{adv("The model is still a prototype: we have not yet trained it with real data, so the next step is an endpoint inside the backend.")}</td></tr>
</table>
<div class="caja violeta" style="font-size:9.5pt">{adv("<b>Real example:</b> when we ran the model on a real EduckTrack class, it just flagged one student with an 83% risk because his average was 2.9 and he had six grades below 3.0.")}</div>

<h3>Online session</h3>
<div style="border:2px dashed #C7CEDB;border-radius:12px;height:46mm;display:flex;align-items:center;justify-content:center;color:#6B7280;font-size:10pt;text-align:center;padding:10px">
  <span class="pendiente">[Paste here the screenshot of the review or of your attendance at the online English session]</span>
</div>
"""

imprimir("GA8_AA1_EV01_IIN_Emerging_Technologies", documento("Emerging Technologies", cuerpo, lang="en"))
