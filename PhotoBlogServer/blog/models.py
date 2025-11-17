from django.db import models

class Post(models.Model):
    title = models.CharField(max_length=200)
    text = models.TextField(blank=True)
    image = models.ImageField(upload_to='photos/')
    created_at = models.DateTimeField(auto_now_add=True)
    def __str__(self): return self.title