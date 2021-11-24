import requests
import json

# headers = 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/96.0.4664.45 Safari/537.36'
# tts_url = 'https://us-central1-sunlit-context-217400.cloudfunctions.net/streamlabs-tts'


class TTSVoice:
    def __init__(self):
        self.headers = 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/96.0.4664.45 Safari/537.36'
        self.url = 'https://us-central1-sunlit-context-217400.cloudfunctions.net/streamlabs-tts'

    def get_audio(self, text: str, voice='Brian'):
        with requests.Session() as s:
            s.headers = self.headers
            data = {"text": text, "voice": voice}
            r = requests.post(self.url, data)
            dump = json.loads(r.text)
            return dump['speak_url']
