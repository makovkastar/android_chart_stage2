package com.opiumfive.telechart.chart.model;

import android.os.Parcel;
import android.os.Parcelable;


public class Viewport implements Parcelable {

    public float left;
    public float top;
    public float right;
    public float bottom;

    public static final Parcelable.Creator<Viewport> CREATOR = new Parcelable.Creator<Viewport>() {

        public Viewport createFromParcel(Parcel in) {
            Viewport v = new Viewport();
            v.readFromParcel(in);
            return v;
        }

        public Viewport[] newArray(int size) {
            return new Viewport[size];
        }
    };

    public Viewport() {
    }

    public Viewport(float left, float top, float right, float bottom) {
        this.left = left;
        this.top = top;
        this.right = right;
        this.bottom = bottom;
    }

    public Viewport(Viewport v) {
        if (v == null) {
            left = top = right = bottom = 0.0f;
        } else {
            left = v.left;
            top = v.top;
            right = v.right;
            bottom = v.bottom;
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        Viewport other = (Viewport) obj;
        if (Float.floatToIntBits(bottom) != Float.floatToIntBits(other.bottom))
            return false;
        if (Float.floatToIntBits(left) != Float.floatToIntBits(other.left))
            return false;
        if (Float.floatToIntBits(right) != Float.floatToIntBits(other.right))
            return false;
        if (Float.floatToIntBits(top) != Float.floatToIntBits(other.top))
            return false;
        return true;
    }

    public final boolean isEmpty() {
        return left >= right || bottom >= top;
    }

    public void setEmpty() {
        left = right = top = bottom = 0;
    }

    public final float width() {
        return right - left;
    }

    public final float height() {
        return top - bottom;
    }

    public final float centerX() {
        return (left + right) * 0.5f;
    }

    public final float centerY() {
        return (top + bottom) * 0.5f;
    }

    public void set(float left, float top, float right, float bottom) {
        this.left = left;
        this.top = top;
        this.right = right;
        this.bottom = bottom;
    }

    public void set(Viewport src) {
        this.left = src.left;
        this.top = src.top;
        this.right = src.right;
        this.bottom = src.bottom;
    }

    public void offset(float dx, float dy) {
        left += dx;
        top += dy;
        right += dx;
        bottom += dy;
    }

    public void offsetTo(float newLeft, float newTop) {
        right += newLeft - left;
        bottom += newTop - top;
        left = newLeft;
        top = newTop;
    }

    public void inset(float dx, float dy) {
        left += dx;
        top -= dy;
        right -= dx;
        bottom += dy;
    }

    public boolean contains(float x, float y) {
        return left < right && bottom < top // check for empty first
                && x >= left && x < right && y >= bottom && y < top;
    }

    public boolean contains(float left, float top, float right, float bottom) {
        return this.left < this.right && this.bottom < this.top && this.left <= left && this.top >= top && this.right >= right && this.bottom <= bottom;
    }

    public boolean contains(Viewport v) {
        return this.left < this.right && this.bottom < this.top && left <= v.left && top >= v.top && right >= v.right && bottom <= v.bottom;
    }

    public void union(float left, float top, float right, float bottom) {
        if ((left < right) && (bottom < top)) {
            if ((this.left < this.right) && (this.bottom < this.top)) {
                if (this.left > left) this.left = left;
                if (this.top < top) this.top = top;
                if (this.right < right) this.right = right;
                if (this.bottom > bottom) this.bottom = bottom;
            } else {
                this.left = left;
                this.top = top;
                this.right = right;
                this.bottom = bottom;
            }
        }
    }

    public void union(Viewport v) {
        union(v.left, v.top, v.right, v.bottom);
    }

    public boolean intersect(float left, float top, float right, float bottom) {
        if (this.left < right && left < this.right && this.bottom < top && bottom < this.top) {
            if (this.left < left) {
                this.left = left;
            }
            if (this.top > top) {
                this.top = top;
            }
            if (this.right > right) {
                this.right = right;
            }
            if (this.bottom < bottom) {
                this.bottom = bottom;
            }
            return true;
        }
        return false;
    }

    public boolean intersect(Viewport v) {
        return intersect(v.left, v.top, v.right, v.bottom);
    }

    @Override
    public String toString() {
        return "Viewport [left=" + left + ", top=" + top + ", right=" + right + ", bottom=" + bottom + "]";
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + Float.floatToIntBits(bottom);
        result = prime * result + Float.floatToIntBits(left);
        result = prime * result + Float.floatToIntBits(right);
        result = prime * result + Float.floatToIntBits(top);
        return result;
    }

    public int describeContents() {
        return 0;
    }

    public void writeToParcel(Parcel out, int flags) {
        out.writeFloat(left);
        out.writeFloat(top);
        out.writeFloat(right);
        out.writeFloat(bottom);
    }

    public void readFromParcel(Parcel in) {
        left = in.readFloat();
        top = in.readFloat();
        right = in.readFloat();
        bottom = in.readFloat();
    }
}
