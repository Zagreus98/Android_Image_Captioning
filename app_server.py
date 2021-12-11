from flask import Flask, render_template, request, redirect, url_for
from werkzeug.utils import secure_filename
import os
from tts import TTSVoice
import json
from caption import caption_image_beam_search
import torch
# from twisted.internet import reactor
import twisted.internet.reactor as reactor
from twisted.web.server import Site
from twisted.web.wsgi import WSGIResource

# Model data
# ------------------------------------------------------------------
device = torch.device("cuda" if torch.cuda.is_available() else "cpu")
model = 'pretrained_embbedings_BEST_checkpoint_coco_5_cap_per_img_5_min_word_freq.pth.tar'
word_map = 'WORDMAP_coco_5_cap_per_img_5_min_word_freq.json'
with open(word_map, 'r') as j:
    word_map = json.load(j)
beam_size = 5
# Load model
checkpoint = torch.load(model, map_location=str(device))
decoder = checkpoint['decoder']
decoder = decoder.to(device)
decoder.eval()
encoder = checkpoint['encoder']
encoder = encoder.to(device)
encoder.eval()
# --------------------------------------------------------------------

CURRENT_FOLDER = r'C:\Users\Mihai\PycharmProjects\Android_Image_Captioning'
UPLOAD_FOLDER = 'uploads'
TTS_AUDIO = 'tts'
ALLOWED_EXTENSIONS = {'png', 'jpg', 'jpeg'}
CWD = os.getcwd()

app = Flask(__name__)
app.config['UPLOAD_FOLDER'] = UPLOAD_FOLDER
app.config['TTS'] = TTS_AUDIO
app.config['CWD'] = CWD


@app.route('/', methods=['GET'])
def index():
    if request.method == "GET":
        return render_template('index.html')


@app.route('/upload', methods=['POST', 'GET'])
def upload_image():
    if request.method == "POST":
        uploaded_file = request.files['file']
        filename = secure_filename(uploaded_file.filename)
        if filename != '':
            uploaded_file.save(os.path.join(app.config['UPLOAD_FOLDER'], filename))
            # uploaded_file.save(os.path.join(CURRENT_FOLDER, filename))
        return redirect(url_for('process_image', image_name=filename))
    else:
        return render_template('upload.html')


@app.route('/process')
def process_image():
    image_name_ext = request.args.get('image_name') # TBD unde punem imaginea ca sa construiesc path-ul catre ea
    image_name = image_name_ext.split('.')[0]
    image_path = os.path.join(app.config['UPLOAD_FOLDER'], image_name_ext)
    caption = caption_image_beam_search(encoder, decoder, image_path, word_map, beam_size)
    print(caption)
    #caption = "This is {}".format(image_name)
    return redirect(url_for('text2speech', image_name=image_name, caption=caption))


@app.route('/tts')
def text2speech():
    image_name = request.args.get('image_name')
    caption = request.args.get('caption')
    tts = TTSVoice()
    audio_url = tts.get_audio(caption)
    json_dict = {
               "image_name": image_name,
               "caption": caption,
               "audio_url": audio_url
    }
    tts_json = json.dumps(json_dict)
    with open(os.path.join(app.config['CWD'], app.config['TTS'], image_name + '.json'), mode='w') as file:
        file.write(tts_json)
        file.close()
    return 'Ok'


@app.route('/check/<image_name>', methods=['GET'])
def checker(image_name):
    if request.method == 'GET':
        # Test what data comes from android
        # data_img_name = request.get_data()
        data_img_name = image_name
        data_img_name = data_img_name.split('.')[0]

        #try:
        json_file_path = os.path.join(app.config['CWD'], app.config['TTS'], data_img_name + '.json')
        with open(json_file_path, mode='r') as file:
            json_file = file.read()
            file.close()
        return json_file

        #except:
            #return 'not_ready'


if __name__ == '__main__':
    app.run()
