from rest_framework import serializers
from .models import Post
from django.contrib.auth.models import User
class PostSerializer(serializers.ModelSerializer):
    class Meta:
        model = Post
        fields = ['id','title','text','image','created_at']

class UserSerializer(serializers.ModelSerializer):
    class Meta:
        model = User
        fields = ['id', 'username', 'email']