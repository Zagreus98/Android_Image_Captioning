from flask import Flask, render_template, request, redirect, url_for
from werkzeug.utils import secure_filename
import os
from tts import TTSVoice
import json
from caption import caption_image_beam_search
import torch

# Model data
# ------------------------------------------------------------------
device = torch.device("cuda" if torch.cuda.is_available() else "cpu")
model = 'pretrained_embbedings_BEST_checkpoint_coco_5_cap_per_img_5_min_word_freq.pth.tar'
word_map = 'WORDMAP_coco_5_cap_per_img_5_min_word_freq.json'
with open(word_map, 'r') as j:
    word_map = json.load(j)
beam_size = 3
# Load model
checkpoint = torch.load(model, map_location=str(device))
decoder = checkpoint['decoder']
decoder = decoder.to(device)
decoder.eval()
encoder = checkpoint['encoder']
encoder = encoder.to(device)
encoder.eval()
# --------------------------------------------------------------------
UPLOAD_FOLDER = 'uploads'
TTS_AUDIO = 'tts'
ALLOWED_EXTENSIONS = {'png', 'jpg', 'jpeg'}
CWD = os.getcwd()

app = Flask(__name__)
app.config['UPLOAD_FOLDER'] = UPLOAD_FOLDER
app.config['TTS'] = TTS_AUDIO
app.config['CWD'] = CWD


@app.route('/upload', methods=['POST', 'GET'])
def upload_image():
    if request.method == "POST":
        uploaded_file = request.files['file']
        filename = secure_filename(uploaded_file.filename)
        if filename != '':
            uploaded_file.save(os.path.join(app.config['UPLOAD_FOLDER'], filename))
        return redirect(url_for('process_image', image_name=filename))
    else:
        return render_template('upload.html')


@app.route('/process')
def process_image():
    image_name_ext = request.args.get('image_name') # TBD unde punem imaginea ca sa construiesc path-ul catre ea
    image_name = image_name_ext.split('.')[0]
    caption = caption_image_beam_search(encoder, decoder, image_name_ext, word_map, beam_size)
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
        data_img_name = request.get_data()

        try:
            json_file_path = os.path.join(app.config['CWD'], app.config['TTS'], data_img_name + '.json')
            with open(json_file_path, os.O_RDONLY) as file:
                json_file = file.read()
                file.close()
            return json_file

        except:
            return 'not_ready'


'''
@app.route('/')
def upload_file_():
    return render_template('upload.html')


@app.route('/', methods=['POST'])
def upload_files():
    uploaded_file = request.files['file']
    filename = secure_filename(uploaded_file.filename)
    if filename != '':
        uploaded_file.save(os.path.join(app.config['UPLOAD_FOLDER'], filename))
    return redirect(url_for('deep_out', image_name=filename))


@app.route('/machine_out/<image_name>', methods=['GET', 'POST'])
def deep_out(image_name):
    """Call to deep learning model with the file path - return the caption"""

    caption = 'PLACEHOLDER'
    return redirect(url_for('do_tts', image_name=image_name, caption=caption))


@app.route('/do_tts/<image_name>/<caption>', methods=['GET', 'POST'])
def do_tts(image_name, caption):
    """
    Here we have 2 methods: 1) Upload the audio file to our server and send that to the app
                            2) Just send the audio URL from API (lul)
    """
    # Method 2
    tts = TTSVoice()
    audio_url = tts.get_audio(caption)
    return redirect(url_for('write_tts', image_name=image_name, audio_url=audio_url))


@app.route('/write_tts/<image_name>/<audio_url>', methods=['GET', 'POST'])
def write_tts(image_name, audio_url):
    """
    Save URL to a text file and then read it to send it
    """
    with os.open(os.path.join(app.config['TTS'], image_name + '.txt'), os.O_WRONLY) as file:
        file.write(audio_url)
        file.close()


@app.route('/get_tts', methods=['GET'])
def get_tts(image_name):
    if request.method == 'GET':
        with os.open(os.path.join(app.config['TTS'], image_name + '.txt'), os.O_RDONLY) as file:
            audio_url = file.read()
            file.close()
        return audio_url


@app.route('/test', methods=['GET'])
def test_tts():
    tts = TTSVoice()
    audio_url = str(tts.get_audio("a man standing in front of a tv screen"))

    return redirect(url_for('test_tts_', audio_url=audio_url))


@app.route('/test_out')
def test_tts_():
    audio_url = request.args.get('audio_url')
    #audio_url = 'https://polly.streamlabs.com/v1/speech?OutputFormat=mp3&Text=testing&VoiceId=Brian&X-Amz-Algorithm=AWS4-HMAC-SHA256&X-Amz-Credential=AKIAIHKNQTJ7BGLEFVZA%2F20211122%2Fus-west-2%2Fpolly%2Faws4_request&X-Amz-Date=20211122T160144Z&X-Amz-SignedHeaders=host&X-Amz-Expires=900&X-Amz-Signature=cde86406c35fe9644b9e9aee3868e6a4cf6315af634fee2420e05159163db911'

    return render_template('test.html', variable=audio_url)
'''

if __name__ == '__main__':
    app.run(debug=True)
